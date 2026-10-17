package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
@Service
public class OrderConfirmationWorker {
 private final OrderRepository orders;private final EmailDelivery mail;
 public OrderConfirmationWorker(OrderRepository orders,EmailDelivery mail){this.orders=orders;this.mail=mail;}
 @Transactional public void deliver(String id){
  if(!mail.ready())return;
  var found=orders.lockForShipping(id);if(found.isEmpty())return;var o=found.get();var now=Instant.now();
  if(!"PAID".equals(o.status)||o.confirmationSentAt!=null||(o.confirmationNextAttemptAt!=null&&o.confirmationNextAttemptAt.isAfter(now)))return;
  o.confirmationAttempts++;
  try{mail.sendConfirmation(o);o.confirmationSentAt=Instant.now();o.confirmationNextAttemptAt=null;}
  catch(RuntimeException failure){o.confirmationNextAttemptAt=now.plusSeconds(Math.min(21600L,60L*(1L<<Math.min(o.confirmationAttempts,8))));}
  orders.saveAndFlush(o);
 }
}
