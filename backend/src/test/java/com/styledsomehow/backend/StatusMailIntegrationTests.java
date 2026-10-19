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
  var o=new PendingOrder();o.id=UUID.randomUUID().toString();o.status="PAID";o.fulfilmentStage="PAID";o.paymentMode="LIVE";o.paymentId="pay_fixture";o.email="fixture@gmail.com";o.paidAt=Instant.now();o.createdAt=o.paidAt;o.expiresAt=o.paidAt;orders.saveAndFlush(o);
  fulfilment.advance(o.id,"PAID","PACKING","","owner");fulfilment.advance(o.id,"PACKING","READY_TO_SHIP","","owner");
  assertThat(events.notificationQueue(Instant.now(),PageRequest.of(0,100))).isEmpty();
  o.confirmationSentAt=Instant.now();o.confirmationAttempts=1;orders.saveAndFlush(o);
  fulfilment.courierBooked(o);fulfilment.courierProgress(o,"SHIPPED");fulfilment.courierProgress(o,"IN_TRANSIT");fulfilment.courierProgress(o,"DELIVERY_ISSUE");fulfilment.courierProgress(o,"IN_TRANSIT");orders.flush();events.flush();
  var history=events.findByOrderIdOrderByOccurredAtAsc(o.id);assertThat(history.stream().filter(e->e.notificationEligible).map(e->e.stage)).containsExactly("PACKING","READY_TO_SHIP","SHIPPED","IN_TRANSIT");
  assertThat(events.notificationQueue(Instant.now(),PageRequest.of(0,100))).hasSize(4);
  o.status="REFUNDED";orders.saveAndFlush(o);assertThat(events.notificationQueue(Instant.now(),PageRequest.of(0,100))).isEmpty();
 }
}
