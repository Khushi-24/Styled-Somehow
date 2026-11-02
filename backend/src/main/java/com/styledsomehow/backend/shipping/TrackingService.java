package com.styledsomehow.backend.shipping;
import com.styledsomehow.backend.checkout.OrderRepository;
import com.styledsomehow.backend.fulfilment.FulfilmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
@Service
public class TrackingService {
 private final OrderRepository orders;private final ShipmentRepository shipments;private final TrackingGateway gateway;private final FulfilmentService fulfilment;private final TransactionTemplate tx;
 public TrackingService(OrderRepository orders,ShipmentRepository shipments,TrackingGateway gateway,FulfilmentService fulfilment,PlatformTransactionManager manager){this.orders=orders;this.shipments=shipments;this.gateway=gateway;this.fulfilment=fulfilment;this.tx=new TransactionTemplate(manager);}
 public FulfilmentService.View refresh(String id){
  record Work(String awb){}
  Work work=tx.execute(status->{
   var o=orders.lockForShipping(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));var b=shipments.findById(id).orElse(null);
   if(!"LIVE".equals(o.paymentMode)||b==null||!"BOOKED".equals(b.status)||b.awb==null)return null;
   if(b.trackingTerminal||b.trackingCheckedAt!=null&&b.trackingCheckedAt.isAfter(Instant.now().minusSeconds(60)))return null;
   b.trackingCheckedAt=Instant.now();shipments.saveAndFlush(b);return new Work(b.awb);
  });
  if(work!=null){
   TrackingGateway.Snapshot snapshot=null;try{snapshot=gateway.track(work.awb());}catch(RuntimeException unavailable){/* Keep the last known state; never invent courier movement. */}
   final var result=snapshot;
   tx.executeWithoutResult(status->{
    var o=orders.lockForShipping(id).orElseThrow();var b=shipments.findById(id).orElseThrow();if(!work.awb().equals(b.awb))return;
    b.trackingFailed=result==null;
    if(result!=null){b.courierStatus=result.courierStatus();b.trackingUrl=result.trackingUrl();b.trackingTerminal=result.terminal();if(result.fulfilmentStage()!=null)fulfilment.courierProgress(o,result.fulfilmentStage());}
    shipments.saveAndFlush(b);
   });
  }
  return fulfilment.customer(id);
 }
}
