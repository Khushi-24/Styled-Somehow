package com.styledsomehow.backend.shipping;

import java.math.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Component
public class ShiprocketGateway implements ShippingGateway {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(ShiprocketGateway.class);
 private final ShiprocketClient api;private final String pickup;
 public ShiprocketGateway(ShiprocketClient api,@Value("${shiprocket.pickup-postcode:380015}") String pickup){this.api=api;this.pickup=pickup;}
 public Rate rate(String pin,int quantity,int subtotal){
  if(!pin.matches("[1-9][0-9]{5}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid 6-digit delivery PIN code.");
  if(quantity<1||quantity>2)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"For orders above 2 T-shirts, contact support to confirm packaging and delivery.");
  var result=api.get("/courier/serviceability/",Map.of("pickup_postcode",pickup,"delivery_postcode",pin,"cod",0,"weight","0.5","declared_value",subtotal));
  if(result==null||!result.path("data").path("available_courier_companies").isArray())throw unavailable();
  return selectRate(result.path("data").path("available_courier_companies"));
 }
 public static boolean supportedCourier(String name){String n=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]","");return n.startsWith("delhivery")||n.startsWith("dtdc")||n.startsWith("xpressbees")||n.startsWith("bluedart");}
 public static Rate selectRate(JsonNode couriers){
  Rate best=null;
  for(var c:couriers){
   String name=c.path("courier_name").asText();
   log.info("Shipping option: courier={}, id={}, rate={}, blocked={}",name,c.path("courier_company_id").asInt(0),c.path("rate").asText(),c.path("blocked").asInt(0));
   if(!supportedCourier(name)||c.path("blocked").asInt(0)!=0)continue;
   int id=c.path("courier_company_id").asInt(0);if(id<=0)continue;
   try{var rate=new BigDecimal(c.path("rate").asText());if(rate.signum()<=0)continue;int amount=rate.setScale(0,RoundingMode.CEILING).intValueExact();if(best==null||amount<best.rupees())best=new Rate(id,name,amount,transitDays(c));}catch(NumberFormatException|ArithmeticException ignored){}
  }
  if(best==null)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"We don’t currently deliver to this location. For delivery assistance, email contact@styledsomehow.in.");
  if(best.rupees()>110)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"We don’t currently deliver to this location. For delivery assistance, email contact@styledsomehow.in.");
  return best;
 }
 private static Integer transitDays(JsonNode courier){
  String value=courier.path("estimated_delivery_days").asText().trim();
  if(!value.matches("[0-9]{1,3}"))return null;
  int days=Integer.parseInt(value);return days>0&&days<=90?days:null;
 }
 private static ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cannot check delivery right now. Please try again shortly.");}
}
