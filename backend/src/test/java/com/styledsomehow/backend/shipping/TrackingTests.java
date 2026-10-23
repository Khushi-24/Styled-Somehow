package com.styledsomehow.backend.shipping;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class TrackingTests {
 private tools.jackson.databind.JsonNode payload(String status,String awb)throws Exception{return new tools.jackson.databind.ObjectMapper().readTree("{\"tracking_data\":{\"track_status\":1,\"track_url\":\"https://shiprocket.co/tracking/fixture\",\"shipment_track\":[{\"awb_code\":\""+awb+"\",\"current_status\":\""+status+"\"}]}}");}
 @Test void awbAssignmentIsNotDispatchAndMovementIs()throws Exception{
  assertThat(ShiprocketTrackingGateway.parse(payload("AWB ASSIGNED","fixture"),"fixture").fulfilmentStage()).isNull();
  assertThat(ShiprocketTrackingGateway.parse(payload("PICKED UP","fixture"),"fixture").fulfilmentStage()).isEqualTo("SHIPPED");
  assertThat(ShiprocketTrackingGateway.parse(payload("IN TRANSIT","fixture"),"fixture").fulfilmentStage()).isEqualTo("IN_TRANSIT");
  assertThat(ShiprocketTrackingGateway.parse(payload("DELIVERED","fixture"),"fixture").terminal()).isTrue();
  assertThat(ShiprocketTrackingGateway.parse(payload("RTO DELIVERED","fixture"),"fixture").fulfilmentStage()).isEqualTo("RETURNED_TO_ORIGIN");
 }
 @Test void rejectsTrackingForAnotherParcel()throws Exception{var data=payload("DELIVERED","other");assertThatThrownBy(()->ShiprocketTrackingGateway.parse(data,"fixture")).isInstanceOf(IllegalStateException.class);}
 @Test void acceptsOnlyTrustedHttpsTrackingLinks(){
  assertThat(ShiprocketTrackingGateway.safeUrl("https://shiprocket.co/tracking/123")).isNotNull();
  for(String url:new String[]{"javascript:alert(1)","https://shiprocket.co.evil.test/123","http://shiprocket.co/123","https://user@shiprocket.co/123","https://evil.test"})assertThat(ShiprocketTrackingGateway.safeUrl(url)).isNull();
 }
}
