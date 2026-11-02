package com.styledsomehow.backend.shipping;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
@Component
public class TrackingPoller {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(TrackingPoller.class);
 private final ShipmentRepository shipments;private final TrackingService tracking;
 public TrackingPoller(ShipmentRepository shipments,TrackingService tracking){this.shipments=shipments;this.tracking=tracking;}
 @Scheduled(fixedDelay=60000,initialDelay=75000) public void poll(){
  for(String id:shipments.trackingQueue(Instant.now().minusSeconds(600),PageRequest.of(0,10)))try{tracking.refresh(id);}catch(RuntimeException unavailable){log.warn("Tracking poll failed for order {}; errorType={}",id,unavailable.getClass().getSimpleName());}
 }
}
