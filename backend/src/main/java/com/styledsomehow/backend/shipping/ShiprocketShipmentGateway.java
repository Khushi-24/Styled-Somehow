package com.styledsomehow.backend.shipping;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Component
public class ShiprocketShipmentGateway implements ShipmentGateway {
 private final ShiprocketClient api;
 public ShiprocketShipmentGateway(ShiprocketClient api){this.api=api;}
 public Existing lookup(long providerOrderId,String storeOrderId){
  var order=api.get("/orders/show/"+providerOrderId,Map.of()).path("data");
  if(order.path("id").asLong()!=providerOrderId||!storeOrderId.equals(order.path("channel_order_id").asText()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Shiprocket order does not match this store order reference.");
  String status=order.path("status").asText().trim().toUpperCase(Locale.ROOT);boolean cancelled=List.of("CANCELED","CANCELLED").contains(status);
  var rows=order.path("shipments");var shipment=rows.isArray()&&rows.size()==1?rows.get(0):rows.isObject()?rows:null;
  if(shipment==null||shipment.path("id").asLong()<=0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Shiprocket did not return exactly one shipment. Review manually.");
  long shipmentId=shipment.path("id").asLong();var details=api.get("/shipments/"+shipmentId,Map.of()).path("data");
  if(details.path("id").asLong()!=shipmentId||details.path("order_id").asLong()!=providerOrderId)throw new ResponseStatusException(HttpStatus.CONFLICT,"Shipment ownership could not be verified.");
  String awb=details.path("awb").asText("");if(awb.equals("null"))awb="";String courier=details.path("courier").asText("");int courierId=details.path("sr_courier_id").asInt(0);
  if(!awb.isBlank()&&(!awb.matches("[A-Za-z0-9-]{1,100}")||!ShiprocketGateway.supportedCourier(courier)||courierId<=0))throw new ResponseStatusException(HttpStatus.CONFLICT,"Courier details need manual review.");
  return new Existing(providerOrderId,shipmentId,awb,courier,courierId,cancelled);
 }
 public Created create(Map<String,Object> payload){
  var result=api.post("/orders/create/adhoc",payload);
  long order=result.path("order_id").asLong(0),shipment=result.path("shipment_id").asLong(0);
  if(order<=0||shipment<=0)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Shiprocket did not confirm shipment creation. Review before retrying.");
  return new Created(order,shipment);
 }
 public Assigned assign(long shipmentId,int courierId){
  var result=api.post("/courier/assign/awb",Map.of("shipment_id",shipmentId,"courier_id",courierId));
  var data=result.path("response").path("data");String awb=data.path("awb_code").asText(),name=data.path("courier_name").asText();
  if(result.path("awb_assign_status").asInt(0)!=1||!awb.matches("[A-Za-z0-9-]{1,100}")||!ShiprocketGateway.supportedCourier(name))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Courier assignment needs review in Shiprocket. Do not create another shipment.");
  return new Assigned(awb,name);
 }
}
