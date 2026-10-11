package com.styledsomehow.backend.payments;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.inventory.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class PaymentService {
 private final PaymentGateway gateway;private final CheckoutService checkout;private final OrderRepository orders;private final StockRepository stocks;private final PaymentRepository receipts;private final MovementRepository movements;
 public PaymentService(PaymentGateway gateway,CheckoutService checkout,OrderRepository orders,StockRepository stocks,PaymentRepository receipts,MovementRepository movements){this.gateway=gateway;this.checkout=checkout;this.orders=orders;this.stocks=stocks;this.receipts=receipts;this.movements=movements;}
 public record Start(String keyId,String gatewayOrderId,long amount,String currency,String mode){}
 private void ready(){if(!gateway.ready())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Test payments are not configured yet.");}
 private void lock(){stocks.lockedAll();checkout.expireReservations();}
 public Start start(String id,String owner){ready();lock();var o=checkout.get(id,owner);if(!o.status.equals("PENDING_PAYMENT"))throw new ResponseStatusException(HttpStatus.CONFLICT,"This checkout can no longer start a payment. Refresh its status.");
  long amount=Math.multiplyExact((long)o.total,100);
  if(o.gatewayOrderId==null){var remote=gateway.createOrFindOrder(o.id,amount);if(!remote.id().matches("order_[A-Za-z0-9]+")||remote.amount()!=amount||!remote.currency().equals("INR")||!remote.receipt().equals(o.id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment order does not match checkout");o.gatewayOrderId=remote.id();o.paymentMode="TEST";orders.saveAndFlush(o);}
  return new Start(gateway.keyId(),o.gatewayOrderId,amount,"INR","TEST");
 }
 public PendingOrder verify(String id,String owner,String paymentId,String signature){ready();lock();var o=checkout.get(id,owner);if(o.gatewayOrderId==null||!gateway.verifyCheckout(o.gatewayOrderId,paymentId,signature))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Payment verification failed");var payment=gateway.fetchPayment(paymentId);if(!payment.orderId().equals(o.gatewayOrderId)||!payment.id().equals(paymentId))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Payment does not belong to this checkout");apply(payment);return orders.findById(id).orElseThrow();}
 public PendingOrder refresh(String id,String owner){ready();lock();var o=checkout.get(id,owner);if(o.gatewayOrderId!=null)for(var p:gateway.payments(o.gatewayOrderId))apply(p);return orders.findById(id).orElseThrow();}
 // A verified webhook still fetches the current payment from Razorpay.
 public void webhook(String paymentId){ready();lock();apply(gateway.fetchPayment(paymentId));}
 private void apply(PaymentGateway.Payment payment){
  var found=orders.findByGatewayOrderId(payment.orderId());if(found.isEmpty())return;var o=found.get();
  if(!payment.id().matches("pay_[A-Za-z0-9]+"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid payment reference");
  var previous=receipts.findById(payment.id());
  if(previous.isPresent()){var r=previous.get();if(!r.orderId.equals(o.id))throw new IllegalStateException("Payment ownership mismatch");if(r.status.startsWith("REFUND"))return;
   if((!payment.status().equals("captured")||payment.refundedAmount()>0)&&r.status.equals("PAID")){r.status="REVIEW_REQUIRED";r.reason="Payment was refunded or changed outside this checkout. Review before fulfillment.";r.updatedAt=Instant.now();o.status="PAYMENT_REVIEW";}
   return;
  }
  if(!payment.status().equals("captured")&&!payment.status().equals("refunded"))return;
  var r=new PaymentReceipt();r.id=payment.id();r.orderId=o.id;r.amount=payment.amount();r.createdAt=Instant.now();r.updatedAt=r.createdAt;r.refundKey=UUID.randomUUID().toString();
  if(payment.amount()!=Math.multiplyExact((long)o.total,100)||!payment.currency().equals("INR")||payment.refundedAmount()>0||payment.status().equals("refunded")){
   r.status="REVIEW_REQUIRED";r.reason="Payment amount, currency or refund state differs from checkout. Review manually.";if(o.status.equals("PENDING_PAYMENT"))checkout.cancel(o.id,o.sessionKey);o.status="PAYMENT_REVIEW";receipts.saveAndFlush(r);return;
  }
  var all=stocks.lockedAll();Map<Long,Integer> needed=new LinkedHashMap<>();for(var line:o.items)needed.merge(line.stockId,line.quantity,Integer::sum);
  boolean reserved=o.status.equals("PENDING_PAYMENT");boolean eligible=reserved||o.status.equals("EXPIRED");
  if(o.paymentId!=null||!eligible||needed.entrySet().stream().anyMatch(e->{var s=all.stream().filter(v->v.id.equals(e.getKey())).findFirst().orElseThrow();return reserved?s.reserved<e.getValue()||s.quantity<e.getValue():s.quantity-s.reserved<e.getValue();})){
   r.status="REFUND_PENDING";r.reason=o.paymentId!=null?"Additional payment for an already paid order":"Checkout cancelled or stock unavailable after reservation expiry";
   if(reserved)checkout.cancel(o.id,o.sessionKey);if(o.paymentId==null){o.status="REFUND_PENDING";o.paymentId=payment.id();}receipts.saveAndFlush(r);return;
  }
  for(var entry:needed.entrySet()){
   var stock=all.stream().filter(s->s.id.equals(entry.getKey())).findFirst().orElseThrow();if(reserved)stock.reserved-=entry.getValue();stock.quantity-=entry.getValue();
   var movement=new StockMovement();movement.requestId=UUID.nameUUIDFromBytes((payment.id()+":"+stock.id).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();movement.stockId=stock.id;movement.colour=stock.colour;movement.size=stock.size;movement.delta=-entry.getValue();movement.resultingQuantity=stock.quantity;movement.reason="TEST paid order "+o.id;movement.actor="Razorpay TEST";movement.occurredAt=Instant.now();movements.save(movement);
  }
  o.status="PAID";o.paymentId=payment.id();o.paidAt=Instant.now();r.status="PAID";r.reason="Verified captured payment; stock consumed";stocks.flush();receipts.saveAndFlush(r);orders.saveAndFlush(o);
 }
 public void markChecked(String id){orders.findById(id).ifPresent(o->o.paymentCheckedAt=Instant.now());}
 public void reconcileOrder(String id){if(!gateway.ready())return;lock();var o=orders.findById(id).orElseThrow();if(o.gatewayOrderId!=null)for(var p:gateway.payments(o.gatewayOrderId))apply(p);}
 public void reconcileRefund(String id){if(!gateway.ready())return;lock();var r=receipts.findById(id).orElseThrow();if(!List.of("REFUND_PENDING","REFUND_REQUESTED").contains(r.status))return;
  try{var refund=r.refundId==null?gateway.refund(r.id,r.amount,r.refundKey):gateway.fetchRefund(r.refundId);if(refund.id()==null||!refund.id().matches("rfnd_[A-Za-z0-9]+"))throw new IllegalStateException("Invalid refund reference");r.refundId=refund.id();r.status=refund.status().equals("processed")?"REFUNDED":refund.status().equals("failed")?"REVIEW_REQUIRED":"REFUND_REQUESTED";
   if(refund.status().equals("failed"))r.reason="Refund failed at provider. Manual review required.";var o=orders.findById(r.orderId).orElseThrow();if(o.paymentId!=null&&o.paymentId.equals(r.id)&&!o.status.equals("PAID"))o.status=r.status.equals("REVIEW_REQUIRED")?"PAYMENT_REVIEW":r.status;
  }catch(RuntimeException unavailable){r.retries++;r.reason="Refund needs retry; original payment remains unfulfilled. Check provider account if repeated.";}
  r.updatedAt=Instant.now();receipts.saveAndFlush(r);
 }
 public List<PaymentReceipt> adminReceipts(){return receipts.findTop100ByOrderByCreatedAtDesc();}
}
