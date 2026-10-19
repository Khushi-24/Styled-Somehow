package com.styledsomehow.backend.orderaccess;

import com.styledsomehow.backend.checkout.PendingOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
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
 private void send(String email,String subject,String text){
  if(!ready())throw new IllegalStateException("SMTP is not configured");
  var message=new SimpleMailMessage();message.setFrom(username);message.setReplyTo(username);message.setTo(email);message.setSubject(subject);message.setText(text);client.send(message);
 }
 public void sendCode(String email,String code){send(email,"Your Styled Somehow order access code","Your order access code is "+code+". It expires in 10 minutes. Do not share this code. If you did not request it, ignore this email.");}
 public void sendRecovery(String email,String code,List<String> references){
  send(email,"Your Styled Somehow order references","Your recent paid-order references (newest first):\n"+String.join("\n",references)+"\n\nYour access code for the most recent order is "+code+". It expires in 10 minutes. Enter it on "+storeUrl+"/orders. To view an older order, enter its reference and request a new code. Do not share this code.");
 }
 public void sendConfirmation(PendingOrder order){
  if(!ready())throw new IllegalStateException("SMTP is not configured");
  String orderLink=links.link(order,storeUrl);
  try{
   var message=OrderEmailThread.message(client.getSession(),order.id,null);var helper=new org.springframework.mail.javamail.MimeMessageHelper(message,true,"UTF-8");
   helper.setFrom(new jakarta.mail.internet.InternetAddress(username,"Styled Somehow","UTF-8"));helper.setReplyTo(username);helper.setTo(order.email);
   helper.setSubject(OrderEmailThread.subject(order));
   helper.setText(confirmationText(order,orderLink),OrderEmailTemplate.html(order,orderLink,"cid:styled-somehow-logo"));
   helper.addInline("styled-somehow-logo",new org.springframework.core.io.ClassPathResource("email/logo.png"),"image/png");
   client.send(message);
  }catch(jakarta.mail.MessagingException|java.io.UnsupportedEncodingException failure){throw new IllegalStateException("Could not prepare confirmation email",failure);}
 }
 public void sendStatus(PendingOrder order,com.styledsomehow.backend.fulfilment.FulfilmentEvent event){
  if(!ready())throw new IllegalStateException("SMTP is not configured");
  String label=statusLabel(event.stage),link=links.link(order,storeUrl);
  try{
   var message=OrderEmailThread.message(client.getSession(),order.id,event.id);var helper=new org.springframework.mail.javamail.MimeMessageHelper(message,true,"UTF-8");
   helper.setFrom(new jakarta.mail.internet.InternetAddress(username,"Styled Somehow","UTF-8"));helper.setReplyTo(username);helper.setTo(order.email);helper.setSubject(OrderEmailThread.subject(order));
   String text=("TEST".equals(order.paymentMode)?"TEST ORDER — this order will not be dispatched.\n\n":"")+label+"\n"+statusDescription(event.stage)+"\n\nOrder: "+order.id+"\nView order: "+link+"\nKeep this private link safe. Need help? contact@styledsomehow.in";
   String html="<!doctype html><html lang='en'><body style='margin:0;background:#f5f4f2;font-family:Arial,sans-serif;color:#161616'><table role='presentation' width='100%'><tr><td align='center' style='padding:24px 12px'><table role='presentation' width='600' style='width:100%;max-width:600px;background:white;border:1px solid #e4e1dd;border-radius:10px'><tr><td style='padding:28px 24px'><img src='cid:styled-somehow-logo' width='130' alt='Styled Somehow'><h1 style='font-size:24px;color:#287140'>"+OrderEmailTemplate.escape(label)+"</h1><p style='font-size:14px;line-height:1.7'>"+OrderEmailTemplate.escape(statusDescription(event.stage))+"</p>"+("TEST".equals(order.paymentMode)?"<p style='color:#705018'>TEST ORDER · This order will not be dispatched.</p>":"")+"<p style='font-size:12px;word-break:break-all'>Order "+OrderEmailTemplate.escape(order.id)+"</p><a href='"+OrderEmailTemplate.escape(link)+"' style='display:block;text-align:center;background:#161616;color:white;padding:15px;text-decoration:none;border-radius:5px'>View order</a><p style='font-size:12px;color:#777;line-height:1.7'>Keep this private link safe. Need help? Email contact@styledsomehow.in.</p></td></tr></table></td></tr></table></body></html>";
   helper.setText(text,html);helper.addInline("styled-somehow-logo",new org.springframework.core.io.ClassPathResource("email/logo.png"),"image/png");client.send(message);
  }catch(jakarta.mail.MessagingException|java.io.UnsupportedEncodingException failure){throw new IllegalStateException("Could not prepare order update",failure);}
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
  text.append("\n\nView your order: ").append(url).append("\nThis private link opens your order without a code. Keep this email and do not share the link. It expires 90 days after payment.\n\nNeed help? Email contact@styledsomehow.in.");
  return text.toString();
 }
 private static String rupees(int amount){return String.format(Locale.ROOT,"INR %,d",amount);}
}
