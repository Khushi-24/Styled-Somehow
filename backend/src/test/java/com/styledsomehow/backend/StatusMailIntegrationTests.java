package com.styledsomehow.backend;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.fulfilment.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @Transactional
class StatusMailIntegrationTests {
 @Autowired OrderRepository orders;@Autowired FulfilmentEventRepository events;@Autowired FulfilmentService fulfilment;
 @Test void queueRequiresConfirmationAndSuppressesRepeatedStagesAndInternalBooking(){
  var o=new PendingOrder();o.id=UUID.randomUUID().toString();o.status="PAID";o.fulfilmentStage="PAID";o.paymentMode="LIVE";o.paymentId="pay_fixture";o.email="fixture@gmail.com";o.paidAt=Instant.now();o.createdAt=o.paidAt;o.expiresAt=o.paidAt;o=orders.saveAndFlush(o);
  fulfilment.advance(o.id,"PAID","PACKING","","owner");fulfilment.advance(o.id,"PACKING","READY_TO_SHIP","","owner");
  assertThat(events.notificationQueue(Instant.now(),PageRequest.of(0,100))).isEmpty();
  o.confirmationSentAt=Instant.now();o.confirmationAttempts=1;o=orders.saveAndFlush(o);
  fulfilment.courierBooked(o);fulfilment.courierProgress(o,"SHIPPED");fulfilment.courierProgress(o,"IN_TRANSIT");fulfilment.courierProgress(o,"DELIVERY_ISSUE");fulfilment.courierProgress(o,"IN_TRANSIT");orders.flush();events.flush();
  var history=events.findByOrderIdOrderByOccurredAtAsc(o.id);assertThat(history.stream().filter(e->e.notificationEligible).map(e->e.stage)).containsExactly("PACKING","READY_TO_SHIP","SHIPPED","IN_TRANSIT");
  // A delayed Packing email blocks later stages, even when those are due.
  var packing=history.stream().filter(e->"PACKING".equals(e.stage)).findFirst().orElseThrow();
  assertThat(events.notificationQueue(Instant.now(),PageRequest.of(0,100))).containsExactly(packing.id);
  packing.notificationNextAttemptAt=Instant.now().plusSeconds(3600);events.saveAndFlush(packing);
  assertThat(events.notificationQueue(Instant.now(),PageRequest.of(0,100))).isEmpty();
  packing.notificationSentAt=Instant.now();events.saveAndFlush(packing);
  var ready=history.stream().filter(e->"READY_TO_SHIP".equals(e.stage)).findFirst().orElseThrow();
  assertThat(events.notificationQueue(Instant.now(),PageRequest.of(0,100))).containsExactly(ready.id);
  o.status="REFUNDED";o=orders.saveAndFlush(o);assertThat(events.notificationQueue(Instant.now(),PageRequest.of(0,100))).isEmpty();
 }
 @Test void historicOrderQueuesNewUpdatesWithoutReplayingOldEvents(){
  var o=new PendingOrder();o.id=UUID.randomUUID().toString();o.status="PAID";o.fulfilmentStage="PAID";o.paymentMode="LIVE";o.paymentId="pay_historic";o.email="fixture@gmail.com";o.paidAt=Instant.now();o.createdAt=o.paidAt;o.expiresAt=o.paidAt;o.confirmationSentAt=o.paidAt;o.confirmationAttempts=0;o=orders.saveAndFlush(o);
  var old=new FulfilmentEvent();old.id=UUID.randomUUID().toString();old.orderId=o.id;old.stage="PAID";old.actor="fixture";old.note="";old.occurredAt=o.paidAt;old.notificationEligible=false;events.saveAndFlush(old);
  fulfilment.advance(o.id,"PAID","PACKING","","owner");events.flush();
  var packing=events.findByOrderIdOrderByOccurredAtAsc(o.id).stream().filter(e->e.notificationEligible).findFirst().orElseThrow();
  assertThat(events.notificationQueue(Instant.now(),PageRequest.of(0,100))).containsExactly(packing.id);
  packing.notificationSentAt=Instant.now();events.saveAndFlush(packing);
  assertThat(events.notificationQueue(Instant.now(),PageRequest.of(0,100))).isEmpty();
 }

 @Test void newStageTimeIsStrictlyAfterLatestPersistedMicrosecond(){
  var o=new PendingOrder();o.id=UUID.randomUUID().toString();o.status="PAID";o.fulfilmentStage="PAID";o.paymentMode="TEST";o.paymentId="pay_fixture";o.email="fixture@gmail.com";o.paidAt=Instant.now();o.createdAt=o.paidAt;o.expiresAt=o.paidAt;o=orders.saveAndFlush(o);
  var previous=new FulfilmentEvent();previous.id=UUID.randomUUID().toString();previous.orderId=o.id;previous.stage="PAID";previous.actor="fixture";previous.note="";previous.occurredAt=Instant.now().plusSeconds(1).truncatedTo(java.time.temporal.ChronoUnit.MICROS);events.saveAndFlush(previous);
  fulfilment.advance(o.id,"PAID","PACKING","","owner");var packing=events.findByOrderIdOrderByOccurredAtAsc(o.id).stream().filter(e->e.stage.equals("PACKING")).findFirst().orElseThrow();assertThat(packing.occurredAt).isAfter(previous.occurredAt);
 }

}
