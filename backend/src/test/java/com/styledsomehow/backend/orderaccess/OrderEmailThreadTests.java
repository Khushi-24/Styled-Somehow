package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.PendingOrder;
import org.junit.jupiter.api.Test;
import java.util.Properties;
import static org.assertj.core.api.Assertions.*;
class OrderEmailThreadTests {
 @Test void idsSurviveJavaMailSaveChangesAndUpdatesReferenceTheirOrder()throws Exception{
  var session=jakarta.mail.Session.getInstance(new Properties());var confirmation=OrderEmailThread.message(session,"order-fixture",null);confirmation.setText("confirmed");confirmation.saveChanges();
  assertThat(confirmation.getMessageID()).isEqualTo("<order-order-fixture@styledsomehow.in>");
  var update=OrderEmailThread.message(session,"order-fixture","event-fixture");update.setText("packing");update.saveChanges();update.saveChanges();
  assertThat(update.getHeader("References",null)).isEqualTo(confirmation.getMessageID());assertThat(update.getHeader("In-Reply-To",null)).isEqualTo(confirmation.getMessageID());assertThat(update.getMessageID()).isEqualTo("<update-event-fixture@styledsomehow.in>");
 }
 @Test void differentOrdersHaveDifferentThreadsAndHeaderInjectionIsRejected(){
  var a=new PendingOrder();a.id="one";var b=new PendingOrder();b.id="two";assertThat(OrderEmailThread.subject(a)).isNotEqualTo(OrderEmailThread.subject(b));assertThatThrownBy(()->OrderEmailThread.rootId("id\r\nBcc: attacker")).isInstanceOf(IllegalArgumentException.class);
 }
}
