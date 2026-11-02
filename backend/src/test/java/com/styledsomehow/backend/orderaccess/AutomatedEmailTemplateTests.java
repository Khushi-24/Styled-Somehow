package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.PendingOrder;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class AutomatedEmailTemplateTests {
 @Test void otpIsReadableBrandedAndDoesNotInviteAReply(){
  String html=OrderEmailTemplate.otp("123456","cid:styled-somehow-logo");
  assertThat(html).contains("Your login code","123456","Valid for 10 minutes","Do not share this code","cid:styled-somehow-logo","max-width:560px","Please do not reply","send a new email","mailto:contact@styledsomehow.in?subject=Styled%20Somehow%20support");
  assertThatThrownBy(()->OrderEmailTemplate.otp("<script>","logo")).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void everyStatusUsesTheSameBrandedShellAndEscapesOrderData(){
  var o=new PendingOrder();o.id="<unsafe&reference>";o.paymentMode="TEST";
  for(String stage:new String[]{"PACKING","READY_TO_SHIP","SHIPPED","IN_TRANSIT","DELIVERED"}){
   String html=OrderEmailTemplate.status(o,stage,"https://styledsomehow.in/orders?a=1&b=2","cid:styled-somehow-logo");
   assertThat(html).contains("cid:styled-somehow-logo","align='center'","TEST ORDER","View order","&lt;unsafe&amp;reference&gt;","?a=1&amp;b=2","Please do not reply","send a new email").doesNotContain("<unsafe&reference>");
  }
 }
 @Test void confirmationFooterAndPlainTextAlsoRequestANewEmail(){
  var o=new PendingOrder();o.id="fixture";o.paymentMode="TEST";String url="https://styledsomehow.in/orders";
  assertThat(OrderEmailTemplate.html(o,url,"cid:styled-somehow-logo")).contains("Please do not reply","send a new email");
  assertThat(SmtpEmailDelivery.confirmationText(o,url)).contains("Please do not reply","send a new email to contact@styledsomehow.in");
 }
}
