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
  
  return selectRate(fetch(pin,quantity,subtotal,new ParcelRules().parcel(quantity)));
 }
 @Override public List<Rate> options(String pin,int quantity,int subtotal,Parcel parcel){
  if(parcel==null||!parcel.valid())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter valid packed weight and dimensions.");
  return parseRates(fetch(pin,quantity,subtotal,parcel));
 }
 private JsonNode fetch(String pin,int quantity,int subtotal,Parcel parcel){
  if(!pin.matches("[1-9][0-9]{5}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid delivery PIN.");
  new ParcelRules().parcel(quantity);
  Map<String,Object> params=new LinkedHashMap<>(Map.of("pickup_postcode",pickup,"delivery_postcode",pin,"cod",0,"weight",parcel==null?"0.5":Double.toString(parcel.weightKg()),"declared_value",subtotal));
  if(parcel!=null){params.put("length",parcel.lengthCm());params.put("breadth",parcel.breadthCm());params.put("height",parcel.heightCm());}
  var result=api.get("/courier/serviceability/",params);
  if(result==null||!result.path("data").path("available_courier_companies").isArray())throw unavailable();
  return result.path("data").path("available_courier_companies");
 }
 @Override public Diagnostic diagnose(String pin,int quantity,int subtotal,Parcel parcel,long shiprocketOrderId){
  if(shiprocketOrderId<=0||parcel==null||!parcel.valid())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid Shiprocket order ID and parcel measurements.");
  var pinResult=fetch(pin,quantity,subtotal,parcel);
  var orderResult=api.get("/courier/serviceability/",Map.of("pickup_postcode",pickup,"delivery_postcode",pin,"order_id",shiprocketOrderId));
  if(orderResult==null||!orderResult.path("data").path("available_courier_companies").isArray())throw unavailable();
  Map<String,String> request=new LinkedHashMap<>();request.put("pickup_postcode",pickup);request.put("delivery_postcode",pin);request.put("cod","0 (Prepaid)");request.put("declared_value",Integer.toString(subtotal));request.put("weight",Double.toString(parcel.weightKg()));request.put("length",Double.toString(parcel.lengthCm()));request.put("breadth",Double.toString(parcel.breadthCm()));request.put("height",Double.toString(parcel.heightCm()));
  Map<String,String> saved=Map.of();String notice="";
  try{var details=api.get("/orders/show/"+shiprocketOrderId,Map.of());if(details==null||!details.path("data").isObject())notice="Shiprocket did not return order details. Courier responses below remain available.";else saved=orderSnapshot(details.path("data"));}
  catch(ResponseStatusException error){notice="Could not read saved order details. Check the internal Shiprocket order ID and API user's Orders read permission. Courier responses below remain available.";}
  return new Diagnostic(shiprocketOrderId,pickup,pin,diagnosticCouriers(pinResult),diagnosticCouriers(orderResult.path("data").path("available_courier_companies")),request,saved,notice);
 }
 static Map<String,String> orderSnapshot(JsonNode data){
  Map<String,String> result=new LinkedHashMap<>();
  snapshotFields(data,"",result);
  var shipments=data.path("shipments");if(shipments.isArray()){int index=0;for(var shipment:shipments){snapshotFields(shipment,"shipments["+(index++)+"].",result);}}else if(shipments.isObject())snapshotFields(shipments,"shipments.",result);
  var pickup=data.path("pickup_address");for(String key:List.of("pin_code","pincode","pickup_location")){var value=pickup.path(key);if(value.isValueNode()&&!value.isNull())result.put("pickup_address."+key,value.asText());}
  return result;
 }
 private static void snapshotFields(JsonNode node,String prefix,Map<String,String> result){
  for(String key:List.of("id","channel_id","pickup_location","pickup_id","pickup_postcode","pickup_pincode","customer_pincode","shipping_pincode","payment_method","payment_status","cod","sub_total","total","order_value","weight","length","breadth","height","is_return","is_document","is_dg","is_dangerous","courier_id","courier_name","status")){var value=node.path(key);if(value.isValueNode()&&!value.isNull())result.put(prefix+key,value.asText());}
 }
 private static List<DiagnosticCourier> diagnosticCouriers(JsonNode rows){
  List<DiagnosticCourier> result=new ArrayList<>();
  for(var c:rows)result.add(new DiagnosticCourier(c.path("courier_company_id").asInt(0),c.path("courier_name").asText(),c.path("rate").asText(),c.path("freight_charge").asText(),c.path("cod_charges").asText(),c.path("coverage_charges").asText(),c.path("charge_weight").asText(),c.path("rating").asText(),c.path("blocked").asInt(0)));
  return result;
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
