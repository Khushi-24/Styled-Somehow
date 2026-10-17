package com.styledsomehow.backend.shipping;
import java.util.Map;
public interface ShipmentGateway {
 record Created(long orderId,long shipmentId){}
 record Assigned(String awb,String courier){}
 Created create(Map<String,Object> payload);
 Assigned assign(long shipmentId,int courierId);
}
