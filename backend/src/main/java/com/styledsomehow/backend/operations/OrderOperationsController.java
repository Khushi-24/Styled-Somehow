package com.styledsomehow.backend.operations;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.payments.*;
import com.styledsomehow.backend.shipping.*;
import com.styledsomehow.backend.fulfilment.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageRequest;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api/admin")
public class OrderOperationsController {
 private final OrderRepository orders;private final PaymentRepository payments;private final ShipmentRepository shipments;private final FulfilmentEventRepository events;
 public OrderOperationsController(OrderRepository orders,PaymentRepository payments,ShipmentRepository shipments,FulfilmentEventRepository events){this.orders=orders;this.payments=payments;this.shipments=shipments;this.events=events;}
 public record Receipt(String id,String orderId,long amount,String status,String refundId,String reason,int retries,Instant createdAt,String paymentMode){}
 @GetMapping("/payments/search") public org.springframework.data.domain.Slice<Receipt> paymentSearch(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="") String query){if(page<0||page>10000||query.length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);return payments.search(query.trim().toLowerCase(Locale.ROOT),PageRequest.of(page,50)).map(r->new Receipt(r.id,r.orderId,r.amount,r.status,r.refundId,r.reason,r.retries,r.createdAt,orders.findById(r.orderId).map(o->o.paymentMode).orElse(null)));}
 public record RefundRequest(@NotBlank @Size(max=400) String reason,boolean returnReceived){}
 @PostMapping("/orders/{id}/refund") @Transactional public Map<String,String> refund(@PathVariable String id,@Valid @RequestBody RefundRequest request,Principal actor){
  var o=orders.lockForShipping(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if("REFUND_PENDING".equals(o.status)||"REFUND_REQUESTED".equals(o.status)||"REFUNDED".equals(o.status))return Map.of("status",o.status);
  if(!"PAID".equals(o.status)||o.paymentId==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Only a verified paid order can be refunded.");
  String stage=FulfilmentService.stage(o);boolean before=List.of("PAID","PACKING","READY_TO_SHIP","COURIER_BOOKED").contains(stage);
  if(before&&shipments.findById(id).filter(b->!"CANCELLED".equals(b.status)).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"Resolve or cancel the existing courier booking before refunding.");
  if(!before&&!("DELIVERED".equals(stage)||"RETURNED_TO_ORIGIN".equals(stage)))throw new ResponseStatusException(HttpStatus.CONFLICT,"This shipment is still active. Resolve delivery or return first.");
  if(!before&&!request.returnReceived())throw new ResponseStatusException(HttpStatus.CONFLICT,"Confirm that the returned items were received and inspected.");
  var receipt=payments.lock(o.paymentId).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Payment receipt missing. Review manually."));
  if(!"PAID".equals(receipt.status)||receipt.amount!=Math.multiplyExact((long)o.total,100))throw new ResponseStatusException(HttpStatus.CONFLICT,"This payment needs manual review.");
  receipt.status="REFUND_PENDING";receipt.reason="Owner-approved full refund: "+request.reason().trim();receipt.updatedAt=Instant.now();o.status="REFUND_PENDING";
  var e=new FulfilmentEvent();e.id=UUID.randomUUID().toString();e.orderId=id;e.stage="REFUND_REQUESTED";e.actor=actor.getName().substring(0,Math.min(100,actor.getName().length()));e.note=request.reason().trim();e.occurredAt=Instant.now();e.notificationEligible=false;events.save(e);
  return Map.of("status",o.status);
 }
 @GetMapping("/orders/search") public org.springframework.data.domain.Slice<PendingOrder> search(@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page,@RequestParam(defaultValue="") @Size(max=100) String query){if(page<0||page>10000||query.length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);return orders.adminSearch(query.trim().toLowerCase(Locale.ROOT),PageRequest.of(page,50));}
}
