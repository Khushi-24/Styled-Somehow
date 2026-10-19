package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.fulfilment.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class OrderStatusEmailTests {
 @Test void updateWaitsForConfirmationRetriesFailuresAndDoesNotChangePayment(){
  var orders=mock(OrderRepository.class);var events=mock(FulfilmentEventRepository.class);var mail=mock(EmailDelivery.class);when(mail.ready()).thenReturn(true);
  var o=new PendingOrder();o.id="order";o.status="PAID";var e=new FulfilmentEvent();e.id="event";e.orderId=o.id;e.stage="PACKING";e.notificationEligible=true;
  when(events.findById(e.id)).thenReturn(Optional.of(e));when(events.lockMail(e.id)).thenReturn(Optional.of(e));when(orders.lockForShipping(o.id)).thenReturn(Optional.of(o));
  var worker=new OrderStatusEmailWorker(orders,events,mail);worker.deliver(e.id);verify(mail,never()).sendStatus(any(),any());
  o.confirmationSentAt=Instant.now();o.confirmationAttempts=1;doThrow(new IllegalStateException("fixture")).doNothing().when(mail).sendStatus(o,e);
  worker.deliver(e.id);assertThat(e.notificationSentAt).isNull();assertThat(e.notificationNextAttemptAt).isAfter(Instant.now());assertThat(o.status).isEqualTo("PAID");worker.deliver(e.id);verify(mail,times(1)).sendStatus(o,e);
  e.notificationNextAttemptAt=Instant.now().minusSeconds(1);worker.deliver(e.id);worker.deliver(e.id);verify(mail,times(2)).sendStatus(o,e);assertThat(e.notificationSentAt).isNotNull();
 }
 @Test void historicConfirmationMarkerAllowsNewPackingEmailOnlyOnce(){
  var orders=mock(OrderRepository.class);var events=mock(FulfilmentEventRepository.class);var mail=mock(EmailDelivery.class);when(mail.ready()).thenReturn(true);
  var o=new PendingOrder();o.id="historic";o.status="PAID";o.confirmationSentAt=Instant.now();o.confirmationAttempts=0;
  var e=new FulfilmentEvent();e.id="packing";e.orderId=o.id;e.stage="PACKING";e.notificationEligible=true;
  when(events.findById(e.id)).thenReturn(Optional.of(e));when(events.lockMail(e.id)).thenReturn(Optional.of(e));when(orders.lockForShipping(o.id)).thenReturn(Optional.of(o));
  var worker=new OrderStatusEmailWorker(orders,events,mail);worker.deliver(e.id);worker.deliver(e.id);
  verify(mail,times(1)).sendStatus(o,e);assertThat(e.notificationSentAt).isNotNull();assertThat(o.confirmationAttempts).isZero();
 }

}
