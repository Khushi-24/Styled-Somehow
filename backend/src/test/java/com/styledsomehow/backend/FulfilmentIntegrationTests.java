package com.styledsomehow.backend;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.shipping.*;
import com.styledsomehow.backend.fulfilment.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest @AutoConfigureMockMvc
class FulfilmentIntegrationTests {
 @Autowired OrderRepository orders;@Autowired ShipmentRepository shipments;@Autowired FulfilmentService fulfilment;@Autowired TrackingService tracking;@Autowired FulfilmentEventRepository events;@Autowired MockMvc mvc;
 @MockitoBean TrackingGateway courier;
 private String id;
 @BeforeEach void setup(){
  id=UUID.randomUUID().toString();var o=new PendingOrder();o.id=id;o.status="PAID";o.fulfilmentStage="PAID";o.paymentMode="LIVE";o.paymentId="pay_fixture";o.paidAt=Instant.now();o.createdAt=o.paidAt;o.expiresAt=o.paidAt;o.sessionKey="fixture-session";o.email="fixture@gmail.com";o.name="Fixture";o.pinCode="380015";orders.saveAndFlush(o);
 }
 @AfterEach void clean(){shipments.deleteById(id);events.deleteAll(events.findByOrderIdOrderByOccurredAtAsc(id));orders.deleteById(id);}
 private void booked(){var o=orders.findById(id).orElseThrow();o.fulfilmentStage="COURIER_BOOKED";orders.saveAndFlush(o);var b=new ShipmentBooking();b.orderId=id;b.status="BOOKED";b.awb="AWBFIXTURE";b.courier="Delhivery Surface";b.createdAt=Instant.now();b.updatedAt=b.createdAt;shipments.saveAndFlush(b);}
 @Test void adminTransitionsRequireAuthCsrfAndFreshStage()throws Exception{
  String path="/api/admin/orders/"+id+"/fulfilment",body="{\"expectedStage\":\"PAID\",\"stage\":\"PACKING\",\"note\":\"private note\"}";
  mvc.perform(get(path)).andExpect(status().isUnauthorized());
  mvc.perform(post(path).with(user("admin").roles("ADMIN")).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(post(path).with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.fulfilment.stage").value("PACKING"));
  mvc.perform(post(path).with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
  assertThat(events.findByOrderIdOrderByOccurredAtAsc(id)).hasSize(1);
 }
 @Test void customerTrackingIsScopedAndHidesAdminNotes()throws Exception{
  fulfilment.advance(id,"PAID","PACKING","private note","owner");
  String path="/api/order-access/order/"+id+"/fulfilment";mvc.perform(get(path)).andExpect(status().isUnauthorized());
  var session=new MockHttpSession();session.setAttribute("verified-order-id",id);session.setAttribute("verified-order-until",Instant.now().plusSeconds(600));
  String body=mvc.perform(get(path).session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("PACKING")).andReturn().getResponse().getContentAsString();
  assertThat(body).doesNotContain("private note","owner","quotedCost");
  mvc.perform(get("/api/order-access/order/"+UUID.randomUUID()+"/fulfilment").session(session)).andExpect(status().isUnauthorized());
 }
 @Test void trackingAdvancesAndFailurePreservesLastKnownState(){
  booked();when(courier.track("AWBFIXTURE")).thenReturn(new TrackingGateway.Snapshot("IN TRANSIT","https://shiprocket.co/tracking/AWBFIXTURE","IN_TRANSIT",false));
  assertThat(tracking.refresh(id).stage()).isEqualTo("IN_TRANSIT");
  var b=shipments.findById(id).orElseThrow();b.trackingCheckedAt=Instant.now().minusSeconds(61);shipments.saveAndFlush(b);
  when(courier.track("AWBFIXTURE")).thenThrow(new IllegalStateException("fixture unavailable"));
  var result=tracking.refresh(id);assertThat(result.trackingFailed()).isTrue();assertThat(result.courierStatus()).isEqualTo("IN TRANSIT");assertThat(result.stage()).isEqualTo("IN_TRANSIT");
 }
 @Test void deliveredIsTerminalAndTestNeverCallsTracking(){
  booked();when(courier.track("AWBFIXTURE")).thenReturn(new TrackingGateway.Snapshot("DELIVERED",null,"DELIVERED",true));
  assertThat(tracking.refresh(id).stage()).isEqualTo("DELIVERED");tracking.refresh(id);verify(courier,times(1)).track(anyString());
  var o=orders.findById(id).orElseThrow();o.paymentMode="TEST";orders.saveAndFlush(o);clearInvocations(courier);tracking.refresh(id);verifyNoInteractions(courier);
 }
}
