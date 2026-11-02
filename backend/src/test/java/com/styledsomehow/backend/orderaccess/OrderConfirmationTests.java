package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class OrderConfirmationTests {
 private PendingOrder order(){var o=new PendingOrder();o.id="fixture-reference";o.status="PAID";o.email="fixture@gmail.com";o.paymentMode="TEST";o.name="Fixture";o.line1="Street";o.city="Town";o.state="State";o.pinCode="380015";o.subtotal=599;o.shipping=58;o.total=657;var line=new PendingOrder.OrderLine();line.name="Fixture tee";line.colour="White";line.size="S";line.quantity=1;line.unitPrice=599;o.items.add(line);return o;}
 @Test void sentOrderIsNotEmailedTwice(){
  var orders=mock(OrderRepository.class);var mail=mock(EmailDelivery.class);var o=order();when(mail.ready()).thenReturn(true);when(orders.lockForShipping(o.id)).thenReturn(Optional.of(o));
  var worker=new OrderConfirmationWorker(orders,mail);worker.deliver(o.id);worker.deliver(o.id);
  verify(mail,times(1)).sendConfirmation(o);assertThat(o.confirmationSentAt).isNotNull();
 }
 @Test void failureIsRetriedLaterWithoutChangingPayment(){
  var orders=mock(OrderRepository.class);var mail=mock(EmailDelivery.class);var o=order();when(mail.ready()).thenReturn(true);when(orders.lockForShipping(o.id)).thenReturn(Optional.of(o));
  doThrow(new IllegalStateException("unavailable")).doNothing().when(mail).sendConfirmation(o);
  var worker=new OrderConfirmationWorker(orders,mail);worker.deliver(o.id);
  assertThat(o.status).isEqualTo("PAID");assertThat(o.confirmationSentAt).isNull();assertThat(o.confirmationNextAttemptAt).isAfter(Instant.now());
  worker.deliver(o.id);verify(mail,times(1)).sendConfirmation(o);
  o.confirmationNextAttemptAt=Instant.now().minusSeconds(1);worker.deliver(o.id);
  verify(mail,times(2)).sendConfirmation(o);assertThat(o.confirmationSentAt).isNotNull();assertThat(o.confirmationAttempts).isEqualTo(2);
 }
 @Test void nonPaidAndUnconfiguredDoNotSend(){
  var orders=mock(OrderRepository.class);var mail=mock(EmailDelivery.class);var o=order();o.status="PAYMENT_REVIEW";when(mail.ready()).thenReturn(true);when(orders.lockForShipping(o.id)).thenReturn(Optional.of(o));
  new OrderConfirmationWorker(orders,mail).deliver(o.id);verify(mail,never()).sendConfirmation(any());
  when(mail.ready()).thenReturn(false);new OrderConfirmationWorker(orders,mail).deliver(o.id);verify(mail,never()).sendConfirmation(any());
 }
 @Test void messageIncludesRecoveryLinkTotalsAndTestWarning(){
  var text=SmtpEmailDelivery.confirmationText(order(),"https://styledsomehow.in/orders#access=fixture-token");
  assertThat(text).contains("fixture-reference","INR 657","INR 58","Fixture tee","White","S","Qty 1","No real money was charged","will not be dispatched","https://styledsomehow.in/orders#access=fixture-token").doesNotContain("null");
 }
 @Test void htmlEscapesCustomerTextAndIncludesSafeOrderAccessLink(){
  var o=order();o.name="<script>alert(1)</script>";o.items.get(0).name="A & B <tee>";
  String html=OrderEmailTemplate.html(o,"https://styledsomehow.in/orders#access=fixture-token","cid:styled-somehow-logo");
  assertThat(html).contains("&lt;script&gt;","A &amp; B &lt;tee&gt;","cid:styled-somehow-logo","https://styledsomehow.in/orders#access=fixture-token","TEST ORDER","View order").doesNotContain("<script>","A & B <tee>");
 }

}
