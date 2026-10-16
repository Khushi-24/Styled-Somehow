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
  return selectRate(fetch(pin,quantity,subtotal,null));
 }
 @Override public List<Rate> options(String pin,int quantity,int subtotal,Parcel parcel){
  if(parcel==null||!parcel.valid())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter valid packed weight and dimensions.");
  return parseRates(fetch(pin,quantity,subtotal,parcel));
 }
 private JsonNode fetch(String pin,int quantity,int subtotal,Parcel parcel){
  if(!pin.matches("[1-9][0-9]{5}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid delivery PIN.");
  if(quantity<1||quantity>2)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Automatic shipping currently supports up to two T-shirts.");
  Map<String,Object> params=new LinkedHashMap<>(Map.of("pickup_postcode",pickup,"delivery_postcode",pin,"cod",0,"weight",parcel==null?"0.5":Double.toString(parcel.weightKg()),"declared_value",subtotal));
  if(parcel!=null){params.put("length",parcel.lengthCm());params.put("breadth",parcel.breadthCm());params.put("height",parcel.heightCm());}
  var result=api.get("/courier/serviceability/",params);
  if(result==null||!result.path("data").path("available_courier_companies").isArray())throw unavailable();
  return result.path("data").path("available_courier_companies");
 }
 public static boolean supportedCourier(String name){String n=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]","");return n.startsWith("delhivery")||n.startsWith("dtdc")||n.startsWith("xpressbees")||n.startsWith("bluedart");}
 public static Rate selectRate(JsonNode couriers){
  var options=parseRates(couriers);
  Rate best=options.isEmpty()?null:options.get(0);
  if(best==null||best.rupees()>110)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"We don’t currently deliver to this location. For delivery assistance, email contact@styledsomehow.in.");
  return best;
 }
 public static List<Rate> parseRates(JsonNode couriers){
  List<Rate> options=new ArrayList<>();
  for(var c:couriers){
   String name=c.path("courier_name").asText();
   log.info("Shipping option: courier={}, id={}, rate={}, blocked={}",name,c.path("courier_company_id").asInt(0),c.path("rate").asText(),c.path("blocked").asInt(0));
   if(!supportedCourier(name)||c.path("blocked").asInt(0)!=0)continue;
   int id=c.path("courier_company_id").asInt(0);if(id<=0)continue;
   try{var rate=new BigDecimal(c.path("rate").asText());if(rate.signum()<=0)continue;int amount=rate.setScale(0,RoundingMode.CEILING).intValueExact();options.add(new Rate(id,name,amount,transitDays(c),rating(c)));}catch(NumberFormatException|ArithmeticException ignored){}
  }
  return options.stream().sorted(Comparator.comparingInt(Rate::rupees).thenComparingInt(Rate::courierId)).toList();
 }
 private static Double rating(JsonNode courier){
  try{double value=Double.parseDouble(courier.path("rating").asText());return Double.isFinite(value)&&value>0&&value<=5?value:null;}catch(NumberFormatException error){return null;}
 }
 private static Integer transitDays(JsonNode courier){
  String value=courier.path("estimated_delivery_days").asText().trim();
  if(!value.matches("[0-9]{1,3}"))return null;
  int days=Integer.parseInt(value);return days>0&&days<=90?days:null;
 }
 private static ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cannot check delivery right now. Please try again shortly.");}
}
