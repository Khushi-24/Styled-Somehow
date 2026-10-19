package com.styledsomehow.backend.orderaccess;

import com.styledsomehow.backend.checkout.PendingOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Locale;

@Component
public class SmtpEmailDelivery implements EmailDelivery {
 private final JavaMailSenderImpl client;
 private final OrderLinkService links;
 private final String username,password,storeUrl;
 public SmtpEmailDelivery(@Value("${email.smtp.host:smtp.hostinger.com}") String host,
   @Value("${email.smtp.port:465}") int port,@Value("${email.smtp.username:}") String username,
   @Value("${email.smtp.password:}") String password,@Value("${email.store-url:http://localhost:3000}") String storeUrl,OrderLinkService links){
  this.links=links;this.username=username;this.password=password;this.storeUrl=storeUrl.replaceAll("/+$","");
  client=new JavaMailSenderImpl();client.setHost(host);client.setPort(port);client.setUsername(username);client.setPassword(password);client.setDefaultEncoding("UTF-8");
  var p=client.getJavaMailProperties();
  p.setProperty("mail.smtp.auth","true");
  p.setProperty("mail.smtp.ssl.enable",String.valueOf(port==465));
  p.setProperty("mail.smtp.ssl.checkserveridentity","true");
  p.setProperty("mail.smtp.starttls.enable",String.valueOf(port!=465));
  p.setProperty("mail.smtp.starttls.required",String.valueOf(port!=465));
  p.setProperty("mail.smtp.connectiontimeout","5000");p.setProperty("mail.smtp.timeout","10000");p.setProperty("mail.smtp.writetimeout","10000");
 }
 public boolean ready(){return username.contains("@")&&!password.isBlank();}
 private void sendBranded(String email,String subject,String text,String html,String orderId,String eventId){
  if(!ready())throw new IllegalStateException("SMTP is not configured");
  try{
   var message=orderId==null?client.createMimeMessage():OrderEmailThread.message(client.getSession(),orderId,eventId);
   var helper=new org.springframework.mail.javamail.MimeMessageHelper(message,true,"UTF-8");
   helper.setFrom(new jakarta.mail.internet.InternetAddress(username,"Styled Somehow","UTF-8"));helper.setTo(email);helper.setSubject(subject);
   // Discourage automated responses; human Reply buttons are controlled by email clients.
   message.setHeader("Auto-Submitted","auto-generated");message.setHeader("X-Auto-Response-Suppress","All");
   helper.setText(text,html);helper.addInline("styled-somehow-logo",new org.springframework.core.io.ClassPathResource("email/logo.png"),"image/png");client.send(message);
  }catch(jakarta.mail.MessagingException|java.io.UnsupportedEncodingException failure){throw new IllegalStateException("Could not prepare automated email",failure);}
 }
 public void sendCode(String email,String code){
  String text="Your Styled Somehow login code: "+code+"\n\nValid for 10 minutes. Do not share this code. If you did not request it, ignore this email.\n\n"+OrderEmailTemplate.noReplyText();
  sendBranded(email,"Your Styled Somehow login code",text,OrderEmailTemplate.otp(code,"cid:styled-somehow-logo"),null,null);
 }
 public void sendRecovery(String email,String code,List<String> references){
  String text="Your recent order references:\n"+String.join("\n",references)+"\n\nYour access code: "+code+". It expires in 10 minutes. Do not share this code.";
  sendBranded(email,"Your Styled Somehow order references",text+"\n\n"+OrderEmailTemplate.noReplyText(),OrderEmailTemplate.recovery(text,"cid:styled-somehow-logo"),null,null);
 }
 public void sendConfirmation(PendingOrder order){
  String link=links.link(order,storeUrl);sendBranded(order.email,OrderEmailThread.subject(order),confirmationText(order,link),OrderEmailTemplate.html(order,link,"cid:styled-somehow-logo"),order.id,null);
 }
 public void sendStatus(PendingOrder order,com.styledsomehow.backend.fulfilment.FulfilmentEvent event){
  String link=links.link(order,storeUrl);
  String text=("TEST".equals(order.paymentMode)?"TEST ORDER — this order will not be dispatched.\n\n":"")+statusLabel(event.stage)+"\n"+statusDescription(event.stage)+"\n\nOrder: "+order.id+"\nView order: "+link+"\nKeep this private link safe.\n\n"+OrderEmailTemplate.noReplyText();
  sendBranded(order.email,OrderEmailThread.subject(order),text,OrderEmailTemplate.status(order,event.stage,link,"cid:styled-somehow-logo"),order.id,event.id);
 }
 static String statusLabel(String stage){return switch(stage){case "PACKING"->"Packing";case "READY_TO_SHIP"->"Ready to ship";case "SHIPPED"->"Shipped";case "IN_TRANSIT"->"In transit";case "DELIVERED"->"Delivered";default->throw new IllegalArgumentException("Unsupported email stage");};}
 static String statusDescription(String stage){return switch(stage){case "PACKING"->"Getting your tee ready for you.";case "READY_TO_SHIP"->"All packed and waiting for pickup.";case "SHIPPED"->"Your parcel is with the courier.";case "IN_TRANSIT"->"Making its way to your doorstep.";case "DELIVERED"->"Let the outfit plans begin.";default->throw new IllegalArgumentException("Unsupported email stage");};}
 static String confirmationText(PendingOrder o,String url){
  var text=new StringBuilder("Thank you for choosing Styled Somehow!\n\n");
  if("TEST".equals(o.paymentMode))text.append("TEST ORDER: No real money was charged. This order will not be dispatched.\n\n");
  text.append("Order reference: ").append(o.id).append("\nPayment received: ").append(rupees(o.total)).append("\n\nYour items:\n");
  for(var line:o.items)text.append(line.name).append(" | ").append(line.colour).append(" | ").append(line.size).append(" | Qty ").append(line.quantity).append(" | ").append(rupees(line.unitPrice*line.quantity)).append("\n");
  text.append("\nSubtotal: ").append(rupees(o.subtotal)).append("\nShipping: ").append(o.shipping==0?"Free":rupees(o.shipping)).append("\nTotal: ").append(rupees(o.total));
  text.append("\n\nDeliver to:\n").append(o.name).append("\n").append(o.line1);
  if(o.line2!=null&&!o.line2.isBlank())text.append("\n").append(o.line2);
  text.append("\n").append(o.city).append(", ").append(o.state).append(" ").append(o.pinCode);
  text.append("\n\nView your order: ").append(url).append("\nThis private link opens your order without a code. Keep this email and do not share the link. It expires 90 days after payment.\n\n").append(OrderEmailTemplate.noReplyText());
  return text.toString();
 }
 private static String rupees(int amount){return String.format(Locale.ROOT,"INR %,d",amount);}
}
