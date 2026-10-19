package com.styledsomehow.backend.operations;
import com.styledsomehow.backend.checkout.CheckoutService;
import com.styledsomehow.backend.checkout.GmailAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
@Service
public class CommerceGuard {
 private final CommerceControlRepository controls;private final CommerceAttemptRepository attempts;private final String salt;
 public CommerceGuard(CommerceControlRepository controls,CommerceAttemptRepository attempts,@Value("${order-access.secret:}") String salt){this.controls=controls;this.attempts=attempts;this.salt=salt;}
 @Transactional(readOnly=true) public void requireOpen(){if(controls.findById(1L).orElseThrow().ordersPaused)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"We’re temporarily not accepting new orders. Please check back shortly.");}
 @Transactional public void check(String scope,String ip,String browser,String email){
  var control=controls.lock();if("checkout".equals(scope)&&control.ordersPaused)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"We’re temporarily not accepting new orders. Please check back shortly.");
  var now=Instant.now();var since=now.minusSeconds(3600);String ih=hash("ip:"+ip),bh=hash("browser:"+browser),eh=hash("email:"+(email==null?"":GmailAddress.inbox(email.trim().toLowerCase(Locale.ROOT))));boolean checkout="checkout".equals(scope);
  if(attempts.countByScopeAndIpHashAndCreatedAtAfter(scope,ih,since)>=(checkout?20:120)||attempts.countByScopeAndBrowserHashAndCreatedAtAfter(scope,bh,since)>=(checkout?10:60)||checkout&&attempts.countByScopeAndEmailHashAndCreatedAtAfter(scope,eh,since)>=10||attempts.countByScopeAndCreatedAtAfter(scope,since)>=(checkout?300:3000))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many requests. Please wait before trying again.");
  attempts.deleteByCreatedAtBefore(now.minusSeconds(86400));var a=new CommerceAttempt();a.id=UUID.randomUUID().toString();a.scope=scope;a.ipHash=ih;a.browserHash=bh;a.emailHash=eh;a.createdAt=now;attempts.saveAndFlush(a);
 }
 private String hash(String value){return CheckoutService.digest(salt+":"+value);}
}
