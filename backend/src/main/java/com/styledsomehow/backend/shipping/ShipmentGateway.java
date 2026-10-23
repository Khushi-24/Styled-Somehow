package com.styledsomehow.backend.shipping;
import java.util.Map;
public interface ShipmentGateway {
 record Created(long orderId,long shipmentId){}
 record Assigned(String awb,String courier){}
 record Existing(long orderId,long shipmentId,String awb,String courier,int courierId,boolean cancelled){}
 default Existing lookup(long providerOrderId,String storeOrderId){throw new IllegalStateException("Shipment reconciliation is unavailable");}
 Created create(Map<String,Object> payload);
 Assigned assign(long shipmentId,int courierId);
}
