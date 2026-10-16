package com.styledsomehow.backend.shipping;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Component
public class ShiprocketShipmentGateway implements ShipmentGateway {
 private final ShiprocketClient api;
 public ShiprocketShipmentGateway(ShiprocketClient api){this.api=api;}
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
