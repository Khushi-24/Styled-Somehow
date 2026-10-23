package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.PendingOrder;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
/** Stable IDs survive retries. Per-order subjects and RFC reply headers encourage inbox threading. */
final class OrderEmailThread {
 private OrderEmailThread(){}
 static String rootId(String id){return "<order-"+safe(id)+"@styledsomehow.in>";}
 private static String safe(String id){if(id==null||!id.matches("[A-Za-z0-9-]{1,100}"))throw new IllegalArgumentException("Invalid email reference");return id;}
 static String subject(PendingOrder order){return ("TEST".equals(order.paymentMode)?"[TEST] ":"")+"Styled Somehow | Order "+safe(order.id);}
 static MimeMessage message(Session session,String orderId,String eventId)throws MessagingException{
  String root=rootId(orderId);String id=eventId==null?root:"<update-"+safe(eventId)+"@styledsomehow.in>";
  // JavaMail calls updateMessageID during saveChanges, so setting a header alone is insufficient.
  var message=new MimeMessage(session){@Override protected void updateMessageID()throws MessagingException{setHeader("Message-ID",id);}};
  message.setHeader("Message-ID",id);if(eventId!=null){message.setHeader("In-Reply-To",root);message.setHeader("References",root);}return message;
 }
}
