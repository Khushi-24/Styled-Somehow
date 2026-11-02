package com.styledsomehow.backend.shipping;
import java.net.http.HttpClient;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
@Component
public class ShiprocketClient {
 private final String email,password;private final RestClient client;
 private String token="";private Instant expires=Instant.MIN;
 public ShiprocketClient(@Value("${shiprocket.email:}") String email,@Value("${shiprocket.password:}") String password){
  this.email=email;this.password=password;
  var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());factory.setReadTimeout(Duration.ofSeconds(10));
  client=RestClient.builder().baseUrl("https://apiv2.shiprocket.in/v1/external").requestFactory(factory).build();
 }
 private synchronized String token(){
  if(email.isBlank()||password.isBlank())throw unavailable();
  if(!token.isBlank()&&expires.isAfter(Instant.now()))return token;
  try{var result=client.post().uri("/auth/login").contentType(MediaType.APPLICATION_JSON).body(Map.of("email",email,"password",password)).retrieve().body(JsonNode.class);token=result==null?"":result.path("token").asText();if(token.isBlank())throw unavailable();expires=Instant.now().plusSeconds(86400);return token;}catch(RuntimeException error){throw unavailable();}
 }
 private synchronized void invalidate(String used){if(token.equals(used)){token="";expires=Instant.MIN;}}
 public JsonNode get(String endpoint,Map<String,Object> params){
  for(int attempt=0;attempt<2;attempt++){
   String auth=token();
   try{return Objects.requireNonNull(client.get().uri(builder->{builder.path(endpoint);params.forEach((key,value)->builder.queryParam(key,value));return builder.build();}).headers(h->h.setBearerAuth(auth)).retrieve().body(JsonNode.class));}
   catch(HttpClientErrorException error){if(error.getStatusCode().value()==401&&attempt==0){invalidate(auth);continue;}throw unavailable();}
   catch(RuntimeException error){throw unavailable();}
  }
  throw unavailable();
 }
 public JsonNode post(String endpoint,Map<String,Object> payload){
  String auth=token();
  try{return Objects.requireNonNull(client.post().uri(endpoint).headers(h->h.setBearerAuth(auth)).contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(JsonNode.class));}
  catch(RuntimeException error){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Shipment result needs review in Shiprocket. Do not create another shipment.");}
 }
 private static ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cannot connect to Shiprocket right now. Check configuration or try again shortly.");}
}
