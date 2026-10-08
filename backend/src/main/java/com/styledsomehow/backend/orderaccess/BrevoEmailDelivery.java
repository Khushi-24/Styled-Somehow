package com.styledsomehow.backend.orderaccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
@Component
public class BrevoEmailDelivery implements EmailDelivery {
 private final String key,sender;
 private final RestClient client;
 public BrevoEmailDelivery(@Value("${email.brevo-api-key:}") String key,@Value("${email.sender:}") String sender){
  this.key=key;this.sender=sender;
  var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());factory.setReadTimeout(Duration.ofSeconds(10));
  client=RestClient.builder().requestFactory(factory).baseUrl("https://api.brevo.com/v3").build();
 }
 public boolean ready(){return !key.isBlank()&&sender.contains("@");}
 public void sendCode(String email,String code){
  client.post().uri("/smtp/email").header("api-key",key).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
   .body(Map.of("sender",Map.of("email",sender,"name","Styled Somehow"),"to",List.of(Map.of("email",email)),"subject","Your Styled Somehow order access code","textContent","Your order access code is "+code+". It expires in 10 minutes. Do not share this code. If you did not request it, ignore this email."))
   .retrieve().toBodilessEntity();
 }
}
