package com.styledsomehow.backend;
import com.styledsomehow.backend.shipping.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.Map;
class ShippingTests {
 @Test void choosesCheapestApprovedCourierAndSkipsBlocked() throws Exception {
  var node=new JsonMapper().readTree("[{\"courier_name\":\"OtherCourier\",\"courier_company_id\":2,\"rate\":10},{\"courier_name\":\"Delhivery Surface\",\"courier_company_id\":3,\"rate\":100},{\"courier_name\":\"DTDC Surface\",\"courier_company_id\":4,\"rate\":84.25},{\"courier_name\":\"Xpressbees\",\"courier_company_id\":5,\"rate\":60,\"blocked\":1}]");
  assertThat(ShiprocketGateway.selectRate(node)).isEqualTo(new ShippingGateway.Rate(4,"DTDC Surface",85));
 }
 @Test void checksAllBlueDartOptionsInsteadOfRejectingExpensiveFirstOption() throws Exception {
  var node=new JsonMapper().readTree("[{\"courier_name\":\"Blue Dart Air\",\"courier_company_id\":1,\"rate\":137.84},{\"courier_name\":\"Blue Dart Advantage Surface\",\"courier_company_id\":2,\"rate\":98.96}]");
  assertThat(ShiprocketGateway.selectRate(node)).isEqualTo(new ShippingGateway.Rate(2,"Blue Dart Advantage Surface",99));
 }
 @Test void keepsEstimateFromCheapestCourier() throws Exception {
  var node=new JsonMapper().readTree("[{\"courier_name\":\"Blue Dart Air\",\"courier_company_id\":1,\"rate\":137.84,\"estimated_delivery_days\":3},{\"courier_name\":\"Blue Dart Advantage Surface\",\"courier_company_id\":2,\"rate\":98.96,\"estimated_delivery_days\":6}]");
  var rate=ShiprocketGateway.selectRate(node);
  assertThat(rate.rupees()).isEqualTo(99);assertThat(rate.transitDays()).isEqualTo(6);
 }
 @Test void comparisonRetainsAboveLimitOptionsButCheckoutStillRejectsThem() throws Exception {
  var json=new JsonMapper().readTree("[{\"courier_name\":\"Blue Dart Air\",\"courier_company_id\":1,\"rate\":132.84,\"rating\":4.7,\"estimated_delivery_days\":3},{\"courier_name\":\"DTDC\",\"courier_company_id\":2,\"rate\":50,\"blocked\":1}]");
  var options=ShiprocketGateway.parseRates(json);assertThat(options).hasSize(1);assertThat(options.get(0).rating()).isEqualTo(4.7);
  assertThatThrownBy(()->ShiprocketGateway.selectRate(json)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }
 @Test void adminComparisonSendsActualPackedMeasurements() throws Exception {
  var api=mock(ShiprocketClient.class);when(api.get(anyString(),anyMap())).thenReturn(new JsonMapper().readTree("{\"data\":{\"available_courier_companies\":[]}}"));
  new ShiprocketGateway(api,"380015").options("560001",2,1198,new ShippingGateway.Parcel(0.48,30,25,4));
  verify(api).get(eq("/courier/serviceability/"),eq(Map.of("pickup_postcode","380015","delivery_postcode","560001","cod",0,"weight","0.48","declared_value",1198,"length",30.0,"breadth",25.0,"height",4.0)));
 }
 @Test void diagnosticsCompareRawResponsesWithoutBookingOrFiltering() throws Exception {
  var api=mock(ShiprocketClient.class);
  var pin=new JsonMapper().readTree("{\"data\":{\"available_courier_companies\":[{\"courier_company_id\":33,\"courier_name\":\"Xpressbees Air\",\"rate\":49.72}]}}");
  var existing=new JsonMapper().readTree("{\"data\":{\"available_courier_companies\":[{\"courier_company_id\":43,\"courier_name\":\"Delhivery Surface\",\"rate\":66.72,\"blocked\":1}]}}");
  when(api.get(eq("/courier/serviceability/"),anyMap())).thenReturn(pin,existing);
  when(api.get(eq("/orders/show/4076130954"),eq(Map.of()))).thenReturn(new JsonMapper().readTree("{\"data\":{\"customer_pincode\":\"362720\",\"payment_method\":\"prepaid\",\"customer_email\":\"private@example.com\",\"shipments\":[{\"weight\":0.5,\"length\":27.94,\"breadth\":33.02,\"height\":2.54,\"shipping_address\":\"PRIVATE\"}]}}"));
  var result=new ShiprocketGateway(api,"380015").diagnose("362720",1,1200,new ShippingGateway.Parcel(0.5,27.94,33.02,2.54),4076130954L);
  assertThat(result.pinBased().get(0).rate()).isEqualTo("49.72");assertThat(result.orderBased().get(0).courier()).isEqualTo("Delhivery Surface");assertThat(result.orderBased().get(0).blocked()).isEqualTo(1);
  assertThat(result.requestDetails()).containsEntry("declared_value","1200");assertThat(result.orderDetails()).containsEntry("customer_pincode","362720").containsEntry("shipments[0].height","2.54").doesNotContainKeys("customer_email","shipments[0].shipping_address");
  verify(api).get(eq("/courier/serviceability/"),eq(Map.of("pickup_postcode","380015","delivery_postcode","362720","order_id",4076130954L)));
  verify(api).get(eq("/orders/show/4076130954"),eq(Map.of()));verify(api,times(3)).get(anyString(),anyMap());verify(api,never()).post(anyString(),anyMap());
 }
 @Test void allows110ButRejectsAboveLimitAndInvalidRates() throws Exception {
  assertThat(ShiprocketGateway.selectRate(new JsonMapper().readTree("[{\"courier_name\":\"Xpressbees Surface\",\"courier_company_id\":1,\"rate\":110}]" )).rupees()).isEqualTo(110);
  for(String json:new String[]{"[]","[{\"courier_name\":\"Delhivery\",\"courier_company_id\":1}]","[{\"courier_name\":\"DTDC\",\"courier_company_id\":1,\"rate\":110.01}]"})assertThatThrownBy(()->ShiprocketGateway.selectRate(new JsonMapper().readTree(json))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }
 @Test void missingDimensionsBlockBookingBeforeExternalCalls(){
  var rates=mock(ShippingGateway.class);var booking=mock(ShipmentGateway.class);
  var service=new ShipmentService(mock(com.styledsomehow.backend.fulfilment.FulfilmentService.class),mock(com.styledsomehow.backend.checkout.OrderRepository.class),mock(ShipmentRepository.class),rates,booking,mock(org.springframework.transaction.PlatformTransactionManager.class),true,"Home",0,0,0);
  assertThatThrownBy(()->service.book("fixture",85,1)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("dimensions");
  verifyNoInteractions(rates,booking);
 }
 @Test void checkoutUsesSameParcelDefaultsAsAdminAndSupportsLargerCarts() throws Exception {
  var api=mock(ShiprocketClient.class);
  when(api.get(anyString(),anyMap())).thenReturn(new JsonMapper().readTree("{\"data\":{\"available_courier_companies\":[{\"courier_name\":\"DTDC\",\"courier_company_id\":1,\"rate\":70}]}}"));
  var gateway=new ShiprocketGateway(api,"380015");
  for(int quantity:new int[]{1,2,3}){gateway.rate("380015",quantity,599);var parcel=new ParcelRules().parcel(quantity);verify(api).get(eq("/courier/serviceability/"),eq(Map.of("pickup_postcode","380015","delivery_postcode","380015","cod",0,"weight",Double.toString(parcel.weightKg()),"declared_value",599,"length",parcel.lengthCm(),"breadth",parcel.breadthCm(),"height",parcel.heightCm())));}
  assertThatThrownBy(()->gateway.rate("380015",21,599)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }
}
