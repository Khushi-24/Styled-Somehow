package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.GmailAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Service @Transactional
public class EmailLoginService {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(EmailLoginService.class);
 private final ChallengeRepository challenges;private final AccessBudgetRepository budget;private final EmailDelivery mail;private final String secret;
 private final SecureRandom random=new SecureRandom();
 public EmailLoginService(ChallengeRepository challenges,AccessBudgetRepository budget,EmailDelivery mail,@Value("${order-access.secret:}") String secret){this.challenges=challenges;this.budget=budget;this.mail=mail;this.secret=secret;}
 public record CodeRequest(String challengeId,String message,int resendAfterSeconds){}
 private void configured(){if(secret.length()<32||!mail.ready())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Email login is temporarily unavailable. Contact contact@styledsomehow.in.");}
 private String hash(String value){try{var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(GeneralSecurityException e){throw new IllegalStateException("Cannot secure login code");}}
 @Transactional(noRollbackFor=ResponseStatusException.class)
 public CodeRequest request(String email,String browser,String ip){
  email=email.trim().toLowerCase(Locale.ROOT);if(!GmailAddress.valid(email))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use the @gmail.com address entered at checkout.");
  configured();budget.lockBudget();var now=Instant.now();String eh=hash("login-email:"+GmailAddress.inbox(email)),bh=hash("login-browser:"+browser),ih=hash("login-ip:"+ip);
  if(challenges.countByEmailHashAndCreatedAtAfter(eh,now.minusSeconds(3600))>=3)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"You can request 3 codes per hour for this email. Please try later.");
  if(challenges.countByEmailHashAndCreatedAtAfter(eh,now.minusSeconds(60))>0)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Wait 60 seconds before requesting another code.");
  if(challenges.countByIpHashAndCreatedAtAfter(ih,now.minusSeconds(3600))>=20||challenges.countByBrowserHashAndCreatedAtAfter(bh,now.minusSeconds(3600))>=5||challenges.countByCreatedAtAfter(now.minusSeconds(86400))>=200)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many code requests. Please try later.");
  challenges.deleteByCreatedAtBefore(now.minusSeconds(30L*86400));
  var c=new AccessChallenge();c.id=UUID.randomUUID().toString();c.emailHash=eh;c.browserHash=bh;c.ipHash=ih;c.createdAt=now;c.expiresAt=now.plusSeconds(600);
  String code=String.format(Locale.ROOT,"%06d",random.nextInt(1000000));c.codeHash=hash("login-code:"+c.id+":"+code);
  // Send to any valid inbox, even with no orders; never reveal purchase history before verification.
  try{mail.sendCode(email,code);c.loginEmail=email;}catch(RuntimeException failure){challenges.saveAndFlush(c);log.warn("Login email delivery failed; errorType={}. Request allowance retained.",failure.getClass().getSimpleName());throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"We couldn’t send the code right now. Please try again later or contact us.");}
  challenges.saveAndFlush(c);return new CodeRequest(c.id,"Code sent. Check your inbox and spam folder.",60);
 }
 public Optional<String> verify(String id,String code,String browser){
  configured();budget.lockBudget();var found=challenges.findById(id);if(found.isEmpty())return Optional.empty();var c=found.get();
  if(c.loginEmail==null||c.consumed||c.attempts>=5||!c.expiresAt.isAfter(Instant.now())||!c.browserHash.equals(hash("login-browser:"+browser)))return Optional.empty();
  c.attempts++;boolean correct=MessageDigest.isEqual(c.codeHash.getBytes(StandardCharsets.US_ASCII),hash("login-code:"+id+":"+code).getBytes(StandardCharsets.US_ASCII));
  if(!correct)return Optional.empty();c.consumed=true;return Optional.of(c.loginEmail);
 }
}
