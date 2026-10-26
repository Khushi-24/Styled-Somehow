package com.styledsomehow.backend;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.inventory.*;
import com.styledsomehow.backend.payments.*;
import com.styledsomehow.backend.shipping.*;
import com.styledsomehow.backend.operations.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @AutoConfigureMockMvc @Transactional
class TestOrderCleanupIntegrationTests {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired OrderRepository orders;@Autowired StockRepository stocks;@Autowired MovementRepository movements;@Autowired PaymentRepository payments;@Autowired ShipmentRepository shipments;@Autowired EntityManager em;@Autowired TestOrderCleanupService cleanup;
 private String id;private PendingOrder order;private BlankStock stock;
 @BeforeEach void fixture(){id=UUID.randomUUID().toString();stock=stocks.findById(1L).orElseThrow();order=new PendingOrder();order.id=id;order.status="EXPIRED";order.paymentMode="TEST";order.sessionKey="cleanup-fixture";order.createdAt=Instant.now();order.expiresAt=Instant.now().minusSeconds(1);order.total=599;order.email="fixture@gmail.com";var line=new PendingOrder.OrderLine();line.stockId=stock.id;line.slug="cherry-zest-oversized-t-shirt";line.name="Fixture";line.colour=stock.colour;line.size=stock.size;line.quantity=1;line.unitPrice=599;order.items.add(line);orders.saveAndFlush(order);}
 private String path(){return "/api/admin/orders/"+id+"/test-cleanup";}
 private void paid(){order.status="PAID";order.paidAt=Instant.now();order.paymentId="pay_"+id.replace("-","");orders.saveAndFlush(order);var receipt=new PaymentReceipt();receipt.id=order.paymentId;receipt.orderId=id;receipt.status="PAID";receipt.amount=59900;receipt.createdAt=Instant.now();receipt.updatedAt=receipt.createdAt;payments.saveAndFlush(receipt);stock.quantity--;stocks.flush();var m=new StockMovement();m.requestId=UUID.nameUUIDFromBytes((order.paymentId+":"+stock.id).getBytes(StandardCharsets.UTF_8)).toString();m.stockId=stock.id;m.colour=stock.colour;m.size=stock.size;m.delta=-1;m.resultingQuantity=stock.quantity;m.reason="TEST paid order "+id;m.actor="Razorpay TEST";m.occurredAt=Instant.now();movements.saveAndFlush(m);}
 private String body(TestOrderCleanupService.Preview p,boolean restore,boolean confirm)throws Exception{return json.writeValueAsString(new TestOrderCleanupController.Delete(p.fingerprint(),restore,confirm,"DELETE TEST","Remove fixture"));}
 @Test void requiresAdminAndCsrfAndProtectsLiveAndUnassigned()throws Exception{
  mvc.perform(get(path())).andExpect(status().isUnauthorized());mvc.perform(get(path()).with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
  var p=cleanup.preview(id);mvc.perform(delete(path()).with(user("owner").roles("ADMIN")).contentType("application/json").content(body(p,false,false))).andExpect(status().isForbidden());
  order.paymentMode="LIVE";orders.flush();mvc.perform(get(path()).with(user("owner").roles("ADMIN"))).andExpect(status().isConflict());mvc.perform(delete(path()).with(user("owner").roles("ADMIN")).with(csrf()).contentType("application/json").content(body(p,false,false))).andExpect(status().isConflict());
  order.paymentMode=null;orders.flush();assertThatThrownBy(()->cleanup.preview(id)).hasMessageContaining("explicitly marked TEST");assertThat(orders.existsById(id)).isTrue();
 }
 @Test void deletesChildrenRetainsAuditAndStockLedgerAndRestoresOnlyOnce()throws Exception{
  paid();em.createNativeQuery("insert into order_access_challenges(id,order_id,attempts,consumed) values (:c,:o,0,false)").setParameter("c",UUID.randomUUID().toString()).setParameter("o",id).executeUpdate();
  var event=new com.styledsomehow.backend.fulfilment.FulfilmentEvent();event.id=UUID.randomUUID().toString();event.orderId=id;event.stage="PACKING";event.actor="owner";event.note="fixture";event.occurredAt=Instant.now();em.persist(event);em.flush();
  var p=cleanup.preview(id);assertThat(p.restorationAvailable()).isTrue();assertThat(p.challengeCount()).isEqualTo(1);int before=stock.quantity;
  mvc.perform(delete(path()).with(user("owner").roles("ADMIN")).with(csrf()).contentType("application/json").content(body(p,true,true))).andExpect(status().isOk()).andExpect(jsonPath("$.restoredUnits").value(1)).andExpect(jsonPath("$.alreadyDeleted").value(false));
  assertThat(orders.existsById(id)).isFalse();assertThat(stock.quantity).isEqualTo(before+1);assertThat(em.find(TestOrderDeletion.class,id)).isNotNull();
  for(String table:List.of("pending_order_lines","payment_receipts","fulfilment_events","order_access_challenges"))assertThat(((Number)em.createNativeQuery("select count(*) from "+table+" where order_id=:id").setParameter("id",id).getSingleResult()).longValue()).isZero();
  assertThat(movements.findByRequestId(UUID.nameUUIDFromBytes((order.paymentId+":"+stock.id).getBytes(StandardCharsets.UTF_8)).toString())).isPresent();
  mvc.perform(delete(path()).with(user("owner").roles("ADMIN")).with(csrf()).contentType("application/json").content(body(p,true,true))).andExpect(status().isOk()).andExpect(jsonPath("$.alreadyDeleted").value(true));assertThat(stock.quantity).isEqualTo(before+1);
 }
 @Test void noRestorationDoesNotChangeOnHandAndPendingOnlyReleasesItsOwnReservation(){
  paid();int current=stock.quantity;var p=cleanup.preview(id);cleanup.delete(id,p.fingerprint(),false,false,"DELETE TEST","Remove fixture","owner");assertThat(stock.quantity).isEqualTo(current);assertThat(em.find(TestOrderDeletion.class,id).restoredUnits).isZero();
 }
 @Test void releasesOnlyThisOrdersReservation(){order.status="PENDING_PAYMENT";orders.flush();stock.reserved=3;stocks.flush();int before=stock.quantity;var p=cleanup.preview(id);var r=cleanup.delete(id,p.fingerprint(),false,false,"DELETE TEST","Remove fixture","owner");assertThat(r.releasedUnits()).isEqualTo(1);assertThat(stock.reserved).isEqualTo(2);assertThat(stock.quantity).isEqualTo(before);}
 @Test void stalePreviewWrongConfirmationAndMissingRestockProofAreRejected(){
  paid();var p=cleanup.preview(id);assertThatThrownBy(()->cleanup.delete(id,p.fingerprint(),true,false,"DELETE TEST","Remove fixture","owner")).hasMessageContaining("confirmation");assertThatThrownBy(()->cleanup.delete(id,p.fingerprint(),false,false,"DELETE","Remove fixture","owner")).hasMessageContaining("Type DELETE TEST");
  stock.quantity++;stocks.flush();assertThatThrownBy(()->cleanup.delete(id,p.fingerprint(),true,true,"DELETE TEST","Remove fixture","owner")).hasMessageContaining("changed after the preview");assertThat(orders.existsById(id)).isTrue();
 }
 @Test void unknownDeductionsNeverRestoreStock(){order.status="PAID";order.paidAt=Instant.now();order.paymentId="pay_missing";orders.flush();var p=cleanup.preview(id);assertThat(p.restorationAvailable()).isFalse();assertThatThrownBy(()->cleanup.delete(id,p.fingerprint(),true,true,"DELETE TEST","Remove fixture","owner")).hasMessageContaining("verified test deduction");}
 @Test void activePaymentsRefundsAndShipmentRecordsBlockCleanup(){
  order.gatewayStartUntil=Instant.now().plusSeconds(60);orders.flush();assertThatThrownBy(()->cleanup.preview(id)).hasMessageContaining("preparation");order.gatewayStartUntil=null;paid();var receipt=payments.findById(order.paymentId).orElseThrow();receipt.status="REFUND_PENDING";payments.flush();assertThatThrownBy(()->cleanup.preview(id)).hasMessageContaining("Payment/refund");receipt.status="PAID";payments.flush();
  var b=new ShipmentBooking();b.orderId=id;b.status="CANCELLED";b.courier="Fixture";b.createdAt=Instant.now();b.updatedAt=b.createdAt;shipments.saveAndFlush(b);assertThatThrownBy(()->cleanup.preview(id)).hasMessageContaining("shipment record");assertThat(orders.existsById(id)).isTrue();
 }
}
