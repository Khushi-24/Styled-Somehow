package com.styledsomehow.backend.fulfilment;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.shipping.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class FulfilmentTests {
 private final OrderRepository orders=mock(OrderRepository.class);
 private final ShipmentRepository shipments=mock(ShipmentRepository.class);
 private final FulfilmentEventRepository events=mock(FulfilmentEventRepository.class);
 private final FulfilmentService service=new FulfilmentService(orders,shipments,events);
 private PendingOrder order(String mode){var o=new PendingOrder();o.id="fixture";o.status="PAID";o.fulfilmentStage="PAID";o.paymentMode=mode;o.paidAt=Instant.now();o.paymentId="pay_fixture";when(orders.lockForShipping(o.id)).thenReturn(Optional.of(o));when(orders.findById(o.id)).thenReturn(Optional.of(o));when(shipments.findById(o.id)).thenReturn(Optional.empty());when(events.findByOrderIdOrderByOccurredAtAsc(o.id)).thenReturn(List.of());return o;}
 @Test void preparationUsesOrderedTransitionsAndRejectsStaleUpdates(){
  var o=order("TEST");assertThat(service.advance(o.id,"PAID","PACKING","","admin").fulfilment().stage()).isEqualTo("PACKING");
  assertThatThrownBy(()->service.advance(o.id,"PAID","PACKING","","admin")).hasMessageContaining("changed");
  assertThatThrownBy(()->service.advance(o.id,"PACKING","DELIVERED","","admin")).hasMessageContaining("not available");
  service.advance(o.id,"PACKING","READY_TO_SHIP","","admin");assertThat(o.status).isEqualTo("PAID");
 }
 @Test void testAndHeldOrdersCannotBeDispatched(){
  var o=order("TEST");o.fulfilmentStage="COURIER_BOOKED";
  var b=new ShipmentBooking();b.status="BOOKED";b.awb="fixture";when(shipments.findById(o.id)).thenReturn(Optional.of(b));
  assertThatThrownBy(()->service.advance(o.id,"COURIER_BOOKED","SHIPPED","collected","admin")).hasMessageContaining("not available");
  o.status="REFUNDED";assertThatThrownBy(()->service.advance(o.id,"COURIER_BOOKED","SHIPPED","collected","admin")).hasMessageContaining("verified paid");
 }
 @Test void handoverRequiresAnAwbAndANote(){
  var o=order("LIVE");o.fulfilmentStage="COURIER_BOOKED";
  assertThatThrownBy(()->service.advance(o.id,"COURIER_BOOKED","SHIPPED","collected","admin")).hasMessageContaining("not available");
  var b=new ShipmentBooking();b.status="BOOKED";b.awb="fixture";when(shipments.findById(o.id)).thenReturn(Optional.of(b));
  assertThatThrownBy(()->service.advance(o.id,"COURIER_BOOKED","SHIPPED","","admin")).hasMessageContaining("handover");
  service.advance(o.id,"COURIER_BOOKED","SHIPPED","Collected at 10 AM","admin");assertThat(o.fulfilmentStage).isEqualTo("SHIPPED");
 }
 @Test void repeatedCourierUpdatesAreIdempotentAndCannotUndoDelivery(){
  var o=order("LIVE");o.fulfilmentStage="COURIER_BOOKED";service.courierProgress(o,"SHIPPED");service.courierProgress(o,"SHIPPED");
  verify(events,times(1)).save(any());service.courierProgress(o,"IN_TRANSIT");service.courierProgress(o,"SHIPPED");assertThat(o.fulfilmentStage).isEqualTo("IN_TRANSIT");service.courierProgress(o,"DELIVERED");service.courierProgress(o,"SHIPPED");assertThat(o.fulfilmentStage).isEqualTo("DELIVERED");verify(events,times(3)).save(any());
 }
 @Test void customerViewDoesNotExposeCostsOrInternalNotes()throws Exception{
  var o=order("LIVE");var event=new FulfilmentEvent();event.stage="PACKING";event.actor="owner";event.note="private production note";event.occurredAt=Instant.now();when(events.findByOrderIdOrderByOccurredAtAsc(o.id)).thenReturn(List.of(event));
  String json=new tools.jackson.databind.ObjectMapper().writeValueAsString(service.customer(o.id));
  assertThat(json).contains("PACKING").doesNotContain("private production note","owner","quotedCost");
 }
}
