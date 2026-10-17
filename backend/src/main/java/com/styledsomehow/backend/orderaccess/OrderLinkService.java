package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Read-only order capability. The token is never sufficient to modify an order. */
@Service
public class OrderLinkService {
 private final OrderRepository orders;private final String secret;private final Clock clock;
 private static final long LIFETIME=90L*86400;
 @org.springframework.beans.factory.annotation.Autowired
 public OrderLinkService(OrderRepository orders,@Value("${order-access.secret:}") String secret){this(orders,secret,Clock.systemUTC());}
 OrderLinkService(OrderRepository orders,String secret,Clock clock){this.orders=orders;this.secret=secret;this.clock=clock;}
 private void configured(){if(secret.length()<32)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Order access is unavailable. Contact contact@styledsomehow.in.");}
 private byte[] signature(PendingOrder o,String payload){
  try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return mac.doFinal(("order-view-v1:"+payload+":"+o.email.trim().toLowerCase(Locale.ROOT)+":"+o.paymentId+":"+o.paidAt.getEpochSecond()).getBytes(StandardCharsets.UTF_8));}
  catch(GeneralSecurityException e){throw new IllegalStateException("Cannot sign order access");}
 }
 public String link(PendingOrder o,String storeUrl){
  configured();if(o.paidAt==null||o.paymentId==null||o.email==null)throw new IllegalStateException("Only confirmed purchases can receive an order link");
  long expires=o.paidAt.getEpochSecond()+LIFETIME;String payload=o.id+"."+expires;
  return storeUrl.replaceAll("/+$","")+"/orders#access="+payload+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(signature(o,payload));
 }
 public String verify(String token){
  configured();
  try{
   if(token==null||!token.matches("[a-fA-F0-9-]{36}\\.[0-9]{1,12}\\.[A-Za-z0-9_-]{43}"))throw invalid();
   String[] parts=token.split("\\.");long expires=Long.parseLong(parts[1]);long now=clock.instant().getEpochSecond();
   if(expires<=now||expires>now+LIFETIME)throw invalid();
   var o=orders.findById(parts[0]).orElseThrow(OrderLinkService::invalid);
   if(o.paidAt==null||o.paymentId==null||o.email==null||expires!=o.paidAt.getEpochSecond()+LIFETIME)throw invalid();
   if(!MessageDigest.isEqual(signature(o,parts[0]+"."+parts[1]),Base64.getUrlDecoder().decode(parts[2])))throw invalid();
   return o.id;
  }catch(IllegalArgumentException failure){throw invalid();}
 }
 private static ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"This order link is invalid or expired. Contact contact@styledsomehow.in for help.");}
}
