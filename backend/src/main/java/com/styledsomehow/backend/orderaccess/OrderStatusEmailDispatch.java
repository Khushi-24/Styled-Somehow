package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.fulfilment.FulfilmentEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
@Component @ConditionalOnProperty(name="email.confirmations.enabled",havingValue="true")
public class OrderStatusEmailDispatch {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(OrderStatusEmailDispatch.class);
 private final FulfilmentEventRepository events;private final OrderStatusEmailWorker worker;
 public OrderStatusEmailDispatch(FulfilmentEventRepository events,OrderStatusEmailWorker worker){this.events=events;this.worker=worker;}
 @Scheduled(fixedDelay=30000,initialDelay=45000) public void run(){for(String id:events.notificationQueue(Instant.now(),PageRequest.of(0,10)))try{worker.deliver(id);}catch(RuntimeException failure){log.warn("Order status mail queue failed for event {}; errorType={}",id,failure.getClass().getSimpleName());}}
}
