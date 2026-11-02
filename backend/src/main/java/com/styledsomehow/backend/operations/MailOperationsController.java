package com.styledsomehow.backend.operations;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.fulfilment.*;
import com.styledsomehow.backend.orderaccess.EmailDelivery;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api/admin/mail")
public class MailOperationsController {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(MailOperationsController.class);
 private final OrderRepository orders;private final FulfilmentEventRepository events;private final EmailDelivery mail;private final boolean enabled,linksReady;
 public MailOperationsController(OrderRepository orders,FulfilmentEventRepository events,EmailDelivery mail,@Value("${email.confirmations.enabled:false}") boolean enabled,@Value("${order-access.secret:}") String secret){this.orders=orders;this.events=events;this.mail=mail;this.enabled=enabled;this.linksReady=secret.length()>=32;}
 public record Job(String kind,String id,String orderId,String stage,int attempts,Instant nextRetry){}
 public record Queue(boolean enabled,boolean providerConfigured,boolean linksConfigured,List<Job> jobs){}
 @GetMapping public Queue queue(){var jobs=new ArrayList<Job>();for(var o:orders.confirmationBacklog(PageRequest.of(0,50)))jobs.add(new Job("confirmation",o.id,o.id,"Order confirmed",o.confirmationAttempts,o.confirmationNextAttemptAt));for(var e:events.mailBacklog(PageRequest.of(0,50)))jobs.add(new Job("status",e.id,e.orderId,e.stage,e.notificationAttempts,e.notificationNextAttemptAt));return new Queue(enabled,mail.ready(),linksReady,jobs);}
 @PostMapping("/{kind}/{id}/retry") @Transactional public Map<String,String> retry(@PathVariable String kind,@PathVariable String id,java.security.Principal actor){
  if("confirmation".equals(kind)){var o=orders.lockForShipping(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(!"PAID".equals(o.status)||o.confirmationSentAt!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"This confirmation is not pending.");o.confirmationNextAttemptAt=null;}
  else if("status".equals(kind)){var read=events.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));var o=orders.lockForShipping(read.orderId).orElseThrow();var e=events.lockMail(id).orElseThrow();if(!"PAID".equals(o.status)||!e.notificationEligible||e.notificationSentAt!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"This update is not pending.");e.notificationNextAttemptAt=null;}
  else throw new ResponseStatusException(HttpStatus.BAD_REQUEST);log.info("Admin scheduled mail retry; kind={}; job={}; actor={}",kind,id,actor.getName());return Map.of("message","Retry scheduled. Earlier pending messages and configuration checks still apply.");
 }
}
