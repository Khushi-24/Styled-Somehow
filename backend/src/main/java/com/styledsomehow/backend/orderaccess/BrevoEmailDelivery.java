package com.styledsomehow.backend.orderaccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="email.provider",havingValue="brevo",matchIfMissing=true)
public class BrevoEmailDelivery implements EmailDelivery {
 private final String key,sender;
 private final OrderLinkService links;
 @Value("${email.store-url:http://localhost:3000}") private String storeUrl;
 private final RestClient client;
 public BrevoEmailDelivery(@Value("${email.brevo-api-key:}") String key,@Value("${email.sender:}") String sender,OrderLinkService links){
  this.links=links;this.key=key;this.sender=sender;
  var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());factory.setReadTimeout(Duration.ofSeconds(10));
  client=RestClient.builder().requestFactory(factory).baseUrl("https://api.brevo.com/v3").build();
 }
 public boolean ready(){return !key.isBlank()&&sender.contains("@");}
 public void sendConfirmation(com.styledsomehow.backend.checkout.PendingOrder order){
  String url=storeUrl.replaceAll("/+$","");String orderLink=links.link(order,url);
  client.post().uri("/smtp/email").header("api-key",key).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
   .body(Map.of("sender",Map.of("email",sender,"name","Styled Somehow"),"to",List.of(Map.of("email",order.email)),"subject",("TEST".equals(order.paymentMode)?"[TEST] ":"")+"Your Styled Somehow order is confirmed","textContent",SmtpEmailDelivery.confirmationText(order,orderLink),"htmlContent",OrderEmailTemplate.html(order,orderLink,url+"/images/styled-somehow-logo.webp")))
   .retrieve().toBodilessEntity();
 }
 public void sendRecovery(String email,String code,List<String> references){
  send(email,"Your Styled Somehow order references","Your recent paid-order references (newest first):\n"+String.join("\n",references)+"\n\nYour access code for the most recent order is "+code+". It expires in 10 minutes. Enter it on the View your order page. To view an older order, enter its reference and request a new code. Do not share this code.");
 }
 private void send(String email,String subject,String text){
  client.post().uri("/smtp/email").header("api-key",key).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
   .body(Map.of("sender",Map.of("email",sender,"name","Styled Somehow"),"to",List.of(Map.of("email",email)),"subject",subject,"textContent",text))
   .retrieve().toBodilessEntity();
 }
 public void sendCode(String email,String code){
  client.post().uri("/smtp/email").header("api-key",key).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
   .body(Map.of("sender",Map.of("email",sender,"name","Styled Somehow"),"to",List.of(Map.of("email",email)),"subject","Your Styled Somehow order access code","textContent","Your order access code is "+code+". It expires in 10 minutes. Do not share this code. If you did not request it, ignore this email."))
   .retrieve().toBodilessEntity();
 }
}
