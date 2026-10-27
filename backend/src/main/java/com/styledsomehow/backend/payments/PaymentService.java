package com.styledsomehow.backend.payments;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.inventory.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
@Service
public class PaymentService {
 @org.springframework.beans.factory.annotation.Autowired private com.styledsomehow.backend.operations.CommerceGuard guard;
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entities;
 private java.util.Optional<PendingOrder> lockOrder(String id){var found=orders.lockForShipping(id);found.ifPresent(o->entities.refresh(o,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE));return found;}
 private final org.springframework.transaction.support.TransactionTemplate tx;
 private final PaymentGateway gateway;private final CheckoutService checkout;private final OrderRepository orders;private final StockRepository stocks;private final PaymentRepository receipts;private final MovementRepository movements;
 public PaymentService(PaymentGateway gateway,CheckoutService checkout,OrderRepository orders,StockRepository stocks,PaymentRepository receipts,MovementRepository movements,org.springframework.transaction.PlatformTransactionManager manager){this.tx=new org.springframework.transaction.support.TransactionTemplate(manager);this.tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);this.gateway=gateway;this.checkout=checkout;this.orders=orders;this.stocks=stocks;this.receipts=receipts;this.movements=movements;}
 public record Start(String keyId,String gatewayOrderId,long amount,String currency,String mode){}
 private String mode(){return gateway.keyId().startsWith("rzp_live_")?"LIVE":"TEST";}
 private void ready(){if(!gateway.ready())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Payments are temporarily unavailable.");}
 private PendingOrder owned(String id,String owner){return orders.findById(id).filter(o->o.sessionKey.equals(owner)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 private void applyFetched(PaymentGateway.Payment payment){tx.executeWithoutResult(status->{stocks.lockedAll();checkout.expireReservations();apply(payment);});}
 public Start start(String id,String owner){
  ready();guard.requireOpen();tx.executeWithoutResult(status->checkout.get(id,owner));
  record Claim(long amount,String existing){}
  var claim=tx.execute(status->{var o=lockOrder(id).filter(v->v.sessionKey.equals(owner)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));guard.requireOpen();
   if(!"PENDING_PAYMENT".equals(o.status)||!o.expiresAt.isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.CONFLICT,"This checkout can no longer start a payment. Refresh its status.");
   if(o.paymentMode!=null&&!mode().equals(o.paymentMode))throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment mode changed. Start a new checkout.");
   if(o.gatewayOrderId!=null)return new Claim(Math.multiplyExact((long)o.total,100),o.gatewayOrderId);
   if(o.gatewayStartUntil!=null&&o.gatewayStartUntil.isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment preparation is already running. Please wait before retrying.");
   o.gatewayStartUntil=Instant.now().plusSeconds(60);o.paymentMode=mode();orders.saveAndFlush(o);return new Claim(Math.multiplyExact((long)o.total,100),null);
  });
  if(claim.existing()!=null)return new Start(gateway.keyId(),claim.existing(),claim.amount(),"INR",mode());
  // The durable lease serializes creation without holding any DB locks over provider HTTP.
  var remote=gateway.createOrFindOrder(id,claim.amount());
  if(!remote.id().matches("order_[A-Za-z0-9]+")||remote.amount()!=claim.amount()||!remote.currency().equals("INR")||!remote.receipt().equals(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment order does not match checkout");
  var result=tx.execute(status->{var o=lockOrder(id).orElseThrow();if(o.gatewayOrderId!=null&&!o.gatewayOrderId.equals(remote.id()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment order needs review.");o.gatewayOrderId=remote.id();o.gatewayStartUntil=null;orders.saveAndFlush(o);return "PENDING_PAYMENT".equals(o.status)&&o.expiresAt.isAfter(Instant.now())?new Start(gateway.keyId(),o.gatewayOrderId,claim.amount(),"INR",o.paymentMode):null;});
  if(result==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Checkout changed while preparing payment. Refresh its status.");return result;
 }
 public PendingOrder verify(String id,String owner,String paymentId,String signature){
  ready();var o=tx.execute(status->owned(id,owner));if(o.gatewayOrderId==null||!mode().equals(o.paymentMode)||!gateway.verifyCheckout(o.gatewayOrderId,paymentId,signature))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Payment verification failed");
  var payment=gateway.fetchPayment(paymentId);if(!payment.orderId().equals(o.gatewayOrderId)||!payment.id().equals(paymentId))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Payment does not belong to this checkout");applyFetched(payment);return tx.execute(status->owned(id,owner));
 }
 public PendingOrder refresh(String id,String owner){ready();var o=tx.execute(status->owned(id,owner));if(o.gatewayOrderId!=null&&mode().equals(o.paymentMode))for(var payment:gateway.payments(o.gatewayOrderId))applyFetched(payment);return tx.execute(status->owned(id,owner));}
 public void webhook(String paymentId){ready();applyFetched(gateway.fetchPayment(paymentId));}
 private void apply(PaymentGateway.Payment payment){
  var found=orders.findByGatewayOrderId(payment.orderId());if(found.isEmpty())return;var o=lockOrder(found.get().id).orElseThrow();if(!mode().equals(o.paymentMode))return;
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
   var movement=new StockMovement();movement.requestId=UUID.nameUUIDFromBytes((payment.id()+":"+stock.id).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();movement.stockId=stock.id;movement.colour=stock.colour;movement.size=stock.size;movement.delta=-entry.getValue();movement.resultingQuantity=stock.quantity;movement.reason=o.paymentMode+" paid order "+o.id;movement.actor="Razorpay "+o.paymentMode;movement.occurredAt=Instant.now();movements.save(movement);
  }
  o.status="PAID";o.fulfilmentStage="PAID";o.paymentId=payment.id();o.paidAt=Instant.now();r.status="PAID";r.reason="Verified captured payment; stock consumed";stocks.flush();receipts.saveAndFlush(r);orders.saveAndFlush(o);
 }
 public void markChecked(String id){tx.executeWithoutResult(status->orders.findById(id).ifPresent(o->o.paymentCheckedAt=Instant.now()));}
 public void reconcileOrder(String id){if(!gateway.ready())return;var o=tx.execute(status->orders.findById(id).orElseThrow());if(o.gatewayOrderId!=null&&mode().equals(o.paymentMode))for(var payment:gateway.payments(o.gatewayOrderId))applyFetched(payment);}
 public void reconcileRefund(String id){
  if(!gateway.ready())return;
  record Work(String orderId,String refundId,long amount,String key){}
  var work=tx.execute(status->{var initial=receipts.findById(id).orElseThrow();var o=lockOrder(initial.orderId).orElseThrow();var r=receipts.lock(id).orElseThrow();
   if(!mode().equals(o.paymentMode)||!List.of("REFUND_PENDING","REFUND_REQUESTED").contains(r.status)||r.workerLeaseUntil!=null&&r.workerLeaseUntil.isAfter(Instant.now()))return null;
   r.workerLeaseUntil=Instant.now().plusSeconds(60);receipts.saveAndFlush(r);return new Work(o.id,r.refundId,r.amount,r.refundKey);
  });
  if(work==null)return;
  PaymentGateway.Refund response=null;
  try{response=work.refundId()==null?gateway.refund(id,work.amount(),work.key()):gateway.fetchRefund(work.refundId());if(response==null||response.id()==null||!response.id().matches("rfnd_[A-Za-z0-9]+"))response=null;}catch(RuntimeException unavailable){/* Durable retry below, without private provider payloads. */}
  final var result=response;
  tx.executeWithoutResult(status->{var o=lockOrder(work.orderId()).orElseThrow();var r=receipts.lock(id).orElseThrow();if(!List.of("REFUND_PENDING","REFUND_REQUESTED").contains(r.status))return;
   r.workerLeaseUntil=null;
   if(result==null){r.retries++;r.reason="Refund needs retry; original payment remains unfulfilled. Check provider account if repeated.";}
   else{r.refundId=result.id();r.status="processed".equals(result.status())?"REFUNDED":"failed".equals(result.status())?"REVIEW_REQUIRED":"REFUND_REQUESTED";if("failed".equals(result.status()))r.reason="Refund failed at provider. Manual review required.";if(id.equals(o.paymentId)&&!"PAID".equals(o.status))o.status="REVIEW_REQUIRED".equals(r.status)?"PAYMENT_REVIEW":r.status;}
   r.updatedAt=Instant.now();receipts.saveAndFlush(r);
  });
 }
 public List<PaymentReceipt> adminReceipts(){return receipts.findTop100ByOrderByCreatedAtDesc();}
}
