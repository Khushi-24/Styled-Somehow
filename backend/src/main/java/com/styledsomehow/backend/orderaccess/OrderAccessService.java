package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.security.*;
import java.util.*;
@Service @Transactional
public class OrderAccessService {
 private final ChallengeRepository challenges;private final AccessBudgetRepository budget;private final OrderRepository orders;private final EmailDelivery mail;private final String secret;
 private final SecureRandom random=new SecureRandom();
 public record Result(String challengeId,String message){}
 public OrderAccessService(ChallengeRepository challenges,AccessBudgetRepository budget,OrderRepository orders,EmailDelivery mail,@Value("${order-access.secret:}") String secret){this.challenges=challenges;this.budget=budget;this.orders=orders;this.mail=mail;this.secret=secret;}
 private void configured(){if(secret.length()<32||!mail.ready())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Order email access is not available yet. Please contact the store.");}
 private String hash(String value){try{var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(GeneralSecurityException e){throw new IllegalStateException(e);}}
 public Result request(String orderId,String email,String browser,String ip){
  configured();budget.lockBudget();Instant now=Instant.now();String eh=hash(email.trim().toLowerCase(Locale.ROOT)),bh=hash(browser),ih=hash(ip);
  challenges.deleteByCreatedAtBefore(now.minusSeconds(30L*86400));
  if(challenges.countByCreatedAtAfter(now.minusSeconds(86400))>=200||challenges.countByEmailHashAndCreatedAtAfter(eh,now.minusSeconds(3600))>=5||challenges.countByIpHashAndCreatedAtAfter(ih,now.minusSeconds(3600))>=20||challenges.countByBrowserHashAndCreatedAtAfter(bh,now.minusSeconds(3600))>=5||challenges.countByEmailHashAndCreatedAtAfter(eh,now.minusSeconds(60))>0)
   throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many code requests. Please wait before trying again.");
  var challenge=new AccessChallenge();challenge.id=UUID.randomUUID().toString();challenge.emailHash=eh;challenge.browserHash=bh;challenge.ipHash=ih;challenge.createdAt=now;challenge.expiresAt=now.plusSeconds(600);
  String code=String.format(Locale.ROOT,"%06d",random.nextInt(1000000));challenge.codeHash=hash(challenge.id+":"+code);
  var order=orders.findById(orderId).filter(o->o.email.trim().equalsIgnoreCase(email.trim()));
  if(order.isPresent()){
   try{mail.sendCode(order.get().email.trim(),code);challenge.orderId=orderId;}catch(RuntimeException unavailable){/* Preserve rate limits without logging provider responses or customer data. */}
  }
  challenges.saveAndFlush(challenge);
  return new Result(challenge.id,"If the reference and email match, a code will arrive shortly. Check your spam folder too.");
 }
 // Return failure instead of throwing, so failed attempts are committed.
 public Optional<String> verify(String id,String code,String browser){
  configured();budget.lockBudget();var found=challenges.findById(id);if(found.isEmpty())return Optional.empty();var c=found.get();
  if(!c.browserHash.equals(hash(browser))||c.consumed||c.attempts>=5||!c.expiresAt.isAfter(Instant.now()))return Optional.empty();
  c.attempts++;
  boolean matches=MessageDigest.isEqual(c.codeHash.getBytes(java.nio.charset.StandardCharsets.US_ASCII),hash(c.id+":"+code).getBytes(java.nio.charset.StandardCharsets.US_ASCII));
  if(!matches||c.orderId==null)return Optional.empty();c.consumed=true;return Optional.of(c.orderId);
 }
}
