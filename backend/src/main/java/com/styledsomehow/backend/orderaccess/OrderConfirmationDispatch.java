package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.OrderRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
@Component
@ConditionalOnProperty(name="email.confirmations.enabled",havingValue="true")
public class OrderConfirmationDispatch {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(OrderConfirmationDispatch.class);
 private final OrderRepository orders;private final OrderConfirmationWorker worker;private final EmailDelivery mail;
 public OrderConfirmationDispatch(OrderRepository orders,OrderConfirmationWorker worker,EmailDelivery mail){this.orders=orders;this.worker=worker;this.mail=mail;}
 @Scheduled(fixedDelay=30000,initialDelay=30000) public void run(){
  if(!mail.ready())return;
  for(String id:orders.confirmationQueue(Instant.now(),PageRequest.of(0,10)))try{worker.deliver(id);}catch(RuntimeException unavailable){log.warn("Confirmation email queue could not process order {}; errorType={}. Will retry on a later scan.",id,unavailable.getClass().getSimpleName());}
 }
}
