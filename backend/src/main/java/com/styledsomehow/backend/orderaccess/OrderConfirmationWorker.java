package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
@Service
public class OrderConfirmationWorker {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(OrderConfirmationWorker.class);
 private final OrderRepository orders;private final EmailDelivery mail;
 public OrderConfirmationWorker(OrderRepository orders,EmailDelivery mail){this.orders=orders;this.mail=mail;}
 @Transactional public void deliver(String id){
  if(!mail.ready())return;
  var found=orders.lockForShipping(id);if(found.isEmpty())return;var o=found.get();var now=Instant.now();
  if(!"PAID".equals(o.status)||o.confirmationSentAt!=null||(o.confirmationNextAttemptAt!=null&&o.confirmationNextAttemptAt.isAfter(now)))return;
  o.confirmationAttempts++;
  try{mail.sendConfirmation(o);o.confirmationSentAt=Instant.now();o.confirmationNextAttemptAt=null;log.info("Confirmation email accepted by provider for order {}",id);}
  catch(RuntimeException failure){o.confirmationNextAttemptAt=now.plusSeconds(Math.min(21600L,60L*(1L<<Math.min(o.confirmationAttempts,8))));
   Throwable cause=failure;for(int depth=0;depth<12&&cause.getCause()!=null&&cause.getCause()!=cause;depth++)cause=cause.getCause();
   log.warn("Confirmation email failed for order {}; attempt={}; errorType={}; causeType={}; nextRetry={}. Check SMTP credentials/connectivity, ORDER_ACCESS_SECRET and provider limits. Payment remains unchanged.",id,o.confirmationAttempts,failure.getClass().getSimpleName(),cause.getClass().getSimpleName(),o.confirmationNextAttemptAt);
  }
  orders.saveAndFlush(o);
 }
}
