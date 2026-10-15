package com.styledsomehow.backend.shipping;

import java.net.http.HttpClient;
import java.time.*;
import java.math.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Component
public class ShiprocketGateway implements ShippingGateway {
 private final String email,password,pickup;
 private final RestClient client;
 private String token="";private Instant expires=Instant.MIN;
 public ShiprocketGateway(@Value("${shiprocket.email:}") String email,@Value("${shiprocket.password:}") String password,@Value("${shiprocket.pickup-postcode:380015}") String pickup){
  this.email=email;this.password=password;this.pickup=pickup;
  var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());factory.setReadTimeout(Duration.ofSeconds(10));
  client=RestClient.builder().baseUrl("https://apiv2.shiprocket.in/v1/external").requestFactory(factory).build();
 }
 private synchronized String token(){
  if(email.isBlank()||password.isBlank())throw unavailable();
  if(!token.isBlank()&&expires.isAfter(Instant.now()))return token;
  try{var result=client.post().uri("/auth/login").contentType(MediaType.APPLICATION_JSON).body(Map.of("email",email,"password",password)).retrieve().body(JsonNode.class);
   token=result==null?"":result.path("token").asText();if(token.isBlank())throw unavailable();expires=Instant.now().plusSeconds(86400);return token;
  }catch(RuntimeException e){throw unavailable();}
 }
 private synchronized void invalidate(String used){if(token.equals(used)){token="";expires=Instant.MIN;}}
 public Rate rate(String pin,int quantity,int subtotal){
  if(!pin.matches("[1-9][0-9]{5}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid 6-digit delivery PIN code.");
  if(quantity<1||quantity>2)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"For orders above 2 T-shirts, contact support to confirm packaging and delivery.");
  for(int attempt=0;attempt<2;attempt++){
   String used=token();
   try{var result=client.get().uri(u->u.path("/courier/serviceability/").queryParam("pickup_postcode",pickup).queryParam("delivery_postcode",pin).queryParam("cod",0).queryParam("weight",quantity==1?"0.7":"1.5").queryParam("length",31).queryParam("breadth",39).queryParam("height",quantity==1?4:8).queryParam("declared_value",subtotal).build()).headers(h->h.setBearerAuth(used)).retrieve().body(JsonNode.class);
    if(result==null||!result.path("data").path("available_courier_companies").isArray())throw unavailable();
    return selectRate(result.path("data").path("available_courier_companies"));
   }catch(HttpClientErrorException e){if(e.getStatusCode().value()==401&&attempt==0){invalidate(used);continue;}throw unavailable();}
   catch(ResponseStatusException e){throw e;}catch(RuntimeException e){throw unavailable();}
  }
  throw unavailable();
 }
 public static Rate selectRate(JsonNode couriers){
  Rate best=null;
  for(var c:couriers){
   String name=c.path("courier_name").asText();String normalized=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]","");
   if(!normalized.startsWith("delhivery")||c.path("blocked").asInt(0)!=0)continue;
   int id=c.path("courier_company_id").asInt(0);if(id<=0)continue;
   try{var rate=new BigDecimal(c.path("rate").asText());if(rate.signum()<=0)continue;int amount=rate.setScale(0,RoundingMode.CEILING).intValueExact();if(best==null||amount<best.rupees())best=new Rate(id,name,amount);}catch(NumberFormatException|ArithmeticException ignored){}
  }
  if(best==null)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Delhivery delivery is unavailable for this PIN code. Please use another delivery address.");
  return best;
 }
 private static ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cannot check delivery right now. Please try again shortly.");}
}
