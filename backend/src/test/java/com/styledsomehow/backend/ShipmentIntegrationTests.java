package com.styledsomehow.backend;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.shipping.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"shiprocket.booking-enabled=true","shiprocket.parcel-length-cm=10","shiprocket.parcel-breadth-cm=10","shiprocket.parcel-height-cm=10"}) @AutoConfigureMockMvc
class ShipmentIntegrationTests {
 @Autowired ShipmentService service;@Autowired OrderRepository orders;@Autowired ShipmentRepository shipments;@Autowired MockMvc mvc;
 @MockitoBean ShippingGateway rates;@MockitoBean ShipmentGateway gateway;
 private final List<String> fixtures=new ArrayList<>();
 @BeforeEach void configure(){when(rates.rate(anyString(),anyInt(),anyInt())).thenReturn(new ShippingGateway.Rate(42,"Delhivery Surface",85));when(gateway.create(anyMap())).thenReturn(new ShipmentGateway.Created(101,202));when(gateway.assign(202,42)).thenReturn(new ShipmentGateway.Assigned("AWBFIXTURE202","Delhivery Surface"));}
 @AfterEach void clean(){for(String id:fixtures){shipments.deleteById(id);orders.deleteById(id);}}
 PendingOrder order(String mode,String status){var o=new PendingOrder();o.id=UUID.randomUUID().toString();o.status=status;o.paymentMode=mode;o.paymentId="pay_fixture";o.paidAt=Instant.now();o.createdAt=o.paidAt;o.expiresAt=o.createdAt;o.subtotal=599;o.shipping=85;o.total=684;o.name="Shipping fixture";o.email="fixture@gmail.com";o.phone="9999999999";o.line1="Fixture address";o.city="Ahmedabad";o.state="Gujarat";o.pinCode="380015";var line=new PendingOrder.OrderLine();line.slug="fixture-tee";line.name="Fixture tee";line.colour="White";line.size="S";line.quantity=1;line.unitPrice=599;o.items.add(line);fixtures.add(o.id);return orders.saveAndFlush(o);}
 @Test void blocksTestUnpaidAndRefundOrdersBeforeAnyExternalCall(){for(var pair:List.of(List.of("TEST","PAID"),List.of("LIVE","PENDING_PAYMENT"),List.of("LIVE","REFUNDED"),List.of("LIVE","PAYMENT_REVIEW"))){var o=order(pair.get(0),pair.get(1));assertThatThrownBy(()->service.book(o.id,85,42)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);}verifyNoInteractions(rates,gateway);}
 @Test void bookingIsIdempotentAndKeepsShippingPaymentSeparate(){var o=order("LIVE","PAID");assertThat(service.preview(o.id).quotedCost()).isEqualTo(85);var b=service.book(o.id,85,42);assertThat(b.status).isEqualTo("BOOKED");assertThat(b.awb).isEqualTo("AWBFIXTURE202");assertThat(service.book(o.id,85,42).awb).isEqualTo(b.awb);verify(gateway,times(1)).create(anyMap());verify(gateway,times(1)).assign(202,42);assertThat(orders.findById(o.id).orElseThrow().status).isEqualTo("PAID");}
 @Test void changedQuoteNeverCreatesShipment(){var o=order("LIVE","PAID");assertThatThrownBy(()->service.book(o.id,84,42)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);assertThat(shipments.existsById(o.id)).isFalse();verifyNoInteractions(gateway);}
 @Test void ambiguousCreationIsPersistedForReviewAndNeverRetried(){var o=order("LIVE","PAID");when(gateway.create(anyMap())).thenThrow(new IllegalStateException("timeout"));assertThat(service.book(o.id,85,42).status).isEqualTo("REVIEW_REQUIRED");assertThat(service.book(o.id,85,42).status).isEqualTo("REVIEW_REQUIRED");verify(gateway,times(1)).create(anyMap());verify(gateway,never()).assign(anyLong(),anyInt());}
 @Test void assignmentFailurePreservesRemoteIdsAndBlocksRetries(){var o=order("LIVE","PAID");when(gateway.assign(202,42)).thenThrow(new IllegalStateException("timeout"));var b=service.book(o.id,85,42);assertThat(b.status).isEqualTo("REVIEW_REQUIRED");assertThat(b.providerOrderId).isEqualTo(101L);assertThat(b.providerShipmentId).isEqualTo(202L);service.book(o.id,85,42);verify(gateway,times(1)).assign(202,42);}
 @Test void concurrentBookingSendsOnlyOneCreation() throws Exception{var o=order("LIVE","PAID");var entered=new CountDownLatch(1);var release=new CountDownLatch(1);when(gateway.create(anyMap())).thenAnswer(call->{entered.countDown();if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("fixture timeout");return new ShipmentGateway.Created(101,202);});var pool=Executors.newSingleThreadExecutor();try{var first=pool.submit(()->service.book(o.id,85,42));assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();assertThat(service.book(o.id,85,42).status).isEqualTo("CREATING");release.countDown();assertThat(first.get(5,TimeUnit.SECONDS).status).isEqualTo("BOOKED");verify(gateway,times(1)).create(anyMap());}finally{release.countDown();pool.shutdownNow();}}
 @Test void adminOnlyAndCsrfProtected() throws Exception{var o=order("LIVE","PAID");mvc.perform(get("/api/admin/orders/"+o.id+"/shipment")).andExpect(status().isUnauthorized());mvc.perform(post("/api/admin/orders/"+o.id+"/shipment").with(user("admin").roles("ADMIN")).contentType("application/json").content("{\"expectedCost\":85,\"courierId\":42}")).andExpect(status().isForbidden());mvc.perform(get("/api/admin/orders/"+o.id+"/shipment").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());verifyNoInteractions(gateway);}
 @Test void payloadUsesStoredAmountsAndMeasuredParcel(){var o=order("LIVE","PAID");var body=ShipmentService.payload(o,1,"Home");assertThat(body).containsEntry("sub_total",599).containsEntry("shipping_charges",85).containsEntry("payment_method","Prepaid").containsEntry("weight",0.5).doesNotContainKeys("length","breadth","height");assertThat(ShipmentService.payload(o,2,"Home")).containsEntry("weight",0.5).doesNotContainKeys("length","breadth","height");assertThatThrownBy(()->ShipmentService.payload(o,3,"Home")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);}
}
