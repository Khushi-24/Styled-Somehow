package com.styledsomehow.backend.address;

import com.styledsomehow.backend.checkout.CheckoutDtos.Address;
import com.styledsomehow.backend.shipping.ShiprocketClient;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Service
public class PostalLocationService {
 public record Location(String pinCode,String postalCity,String state,List<String> towns){}
 private record Cached(Location value,Instant expires){}
 private final ShiprocketClient api;
 private final Map<String,Cached> cache=new ConcurrentHashMap<>();
 public PostalLocationService(ShiprocketClient api){this.api=api;}
 public Location lookup(String pin){
  if(pin==null||!pin.matches("[1-9][0-9]{5}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid 6-digit PIN code.");
  var cached=cache.get(pin);if(cached!=null&&cached.expires().isAfter(Instant.now()))return cached.value();
  JsonNode response;
  try{response=api.get("/open/postcode/details",Map.of("postcode",pin));}catch(ResponseStatusException error){throw unavailable();}
  var result=parse(pin,response);
  if(cache.size()>=4096)cache.clear();cache.put(pin,new Cached(result,Instant.now().plusSeconds(86400)));return result;
 }
 public static Location parse(String pin,JsonNode response){
  if(response==null)throw unavailable();
  var details=response.path("postcode_details");
  if(!response.path("success").asBoolean(false)){
   if(details.isMissingNode()&&response.path("message").asText().toLowerCase(Locale.ROOT).contains("not found"))throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"We couldn’t find a location for this PIN code. Check the PIN and try again.");
   throw unavailable();
  }
  String returned=details.path("postcode").asText().trim(),city=details.path("city").asText().trim(),state=details.path("state").asText().trim();
  if(!pin.equals(returned)||!validName(city)||!validName(state))throw unavailable();
  var towns=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);var localities=details.path("locality");
  if(localities.isArray())for(var locality:localities){String name=locality.asText().trim();if(validName(name))towns.add(name);}
  towns.add(city);return new Location(pin,city,state,List.copyOf(towns));
 }
 public void validate(Address address){
  var location=lookup(address.pinCode());
  if(!normal(location.state()).equals(normal(address.state()))||location.towns().stream().noneMatch(town->normal(town).equals(normal(address.city()))))throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"The city or state doesn’t match your PIN code. Check your delivery details again.");
 }
 private static boolean validName(String name){return !name.isBlank()&&name.length()<=100&&!name.equalsIgnoreCase("null");}
 private static String normal(String value){return value==null?"":value.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
 private static ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"We couldn’t verify the city and state right now. Please retry before continuing.");}
}
