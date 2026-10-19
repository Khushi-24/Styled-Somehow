package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.OrderRepository;
import com.styledsomehow.backend.fulfilment.FulfilmentEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
@Service
public class OrderStatusEmailWorker {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(OrderStatusEmailWorker.class);
 private final OrderRepository orders;private final FulfilmentEventRepository events;private final EmailDelivery mail;
 public OrderStatusEmailWorker(OrderRepository orders,FulfilmentEventRepository events,EmailDelivery mail){this.orders=orders;this.events=events;this.mail=mail;}
 @Transactional public void deliver(String id){
  if(!mail.ready())return;
  // Always lock order before its event, matching fulfilment updates and avoiding lock inversion.
  var read=events.findById(id);if(read.isEmpty())return;var order=orders.lockForShipping(read.get().orderId);if(order.isEmpty())return;
  var found=events.lockMail(id);if(found.isEmpty())return;var e=found.get();var o=order.get();var now=Instant.now();
  if(!e.notificationEligible||e.notificationSentAt!=null||(e.notificationNextAttemptAt!=null&&e.notificationNextAttemptAt.isAfter(now))||!"PAID".equals(o.status)||o.confirmationSentAt==null||o.confirmationAttempts==0)return;
  e.notificationAttempts++;
  try{mail.sendStatus(o,e);e.notificationSentAt=Instant.now();e.notificationNextAttemptAt=null;log.info("Order status email accepted by provider for event {}; stage={}",id,e.stage);}
  catch(RuntimeException failure){e.notificationNextAttemptAt=now.plusSeconds(Math.min(21600L,60L*(1L<<Math.min(e.notificationAttempts,8))));log.warn("Order status email failed for event {}; errorType={}; nextRetry={}",id,failure.getClass().getSimpleName(),e.notificationNextAttemptAt);}
  events.saveAndFlush(e);
 }
}
