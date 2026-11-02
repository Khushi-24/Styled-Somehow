package com.styledsomehow.backend.shipping;
public interface TrackingGateway {
 record Snapshot(String courierStatus,String trackingUrl,String fulfilmentStage,boolean terminal){}
 Snapshot track(String awb);
}
