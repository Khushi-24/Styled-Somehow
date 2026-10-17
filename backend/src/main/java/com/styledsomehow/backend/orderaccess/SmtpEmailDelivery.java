package com.styledsomehow.backend.orderaccess;

import com.styledsomehow.backend.checkout.PendingOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Locale;

@Component
@ConditionalOnProperty(name="email.provider",havingValue="smtp")
public class SmtpEmailDelivery implements EmailDelivery {
 private final JavaMailSenderImpl client;
 private final String username,password,storeUrl;
 public SmtpEmailDelivery(@Value("${email.smtp.host:smtp.hostinger.com}") String host,
   @Value("${email.smtp.port:465}") int port,@Value("${email.smtp.username:}") String username,
   @Value("${email.smtp.password:}") String password,@Value("${email.store-url:http://localhost:3000}") String storeUrl){
  this.username=username;this.password=password;this.storeUrl=storeUrl.replaceAll("/+$","");
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
  try{
   var message=client.createMimeMessage();var helper=new org.springframework.mail.javamail.MimeMessageHelper(message,true,"UTF-8");
   helper.setFrom(new jakarta.mail.internet.InternetAddress(username,"Styled Somehow","UTF-8"));helper.setReplyTo(username);helper.setTo(order.email);
   helper.setSubject(("TEST".equals(order.paymentMode)?"[TEST] ":"")+"Your Styled Somehow order is confirmed");
   helper.setText(confirmationText(order,storeUrl),OrderEmailTemplate.html(order,storeUrl,"cid:styled-somehow-logo"));
   helper.addInline("styled-somehow-logo",new org.springframework.core.io.ClassPathResource("email/logo.png"),"image/png");
   client.send(message);
  }catch(jakarta.mail.MessagingException|java.io.UnsupportedEncodingException failure){throw new IllegalStateException("Could not prepare confirmation email",failure);}
 }
 static String confirmationText(PendingOrder o,String url){
  var text=new StringBuilder("Thank you for choosing Styled Somehow!\n\n");
  if("TEST".equals(o.paymentMode))text.append("TEST ORDER: No real money was charged. This order will not be dispatched.\n\n");
  text.append("Order reference: ").append(o.id).append("\nPayment received: ").append(rupees(o.total)).append("\n\nYour items:\n");
  for(var line:o.items)text.append(line.name).append(" | ").append(line.colour).append(" | ").append(line.size).append(" | Qty ").append(line.quantity).append(" | ").append(rupees(line.unitPrice*line.quantity)).append("\n");
  text.append("\nSubtotal: ").append(rupees(o.subtotal)).append("\nShipping: ").append(o.shipping==0?"Free":rupees(o.shipping)).append("\nTotal: ").append(rupees(o.total));
  text.append("\n\nDeliver to:\n").append(o.name).append("\n").append(o.line1);
  if(o.line2!=null&&!o.line2.isBlank())text.append("\n").append(o.line2);
  text.append("\n").append(o.city).append(", ").append(o.state).append(" ").append(o.pinCode);
  text.append("\n\nView your order: ").append(url).append("/orders?reference=").append(o.id).append("\nUse your checkout email and a one-time code to access order details. Keep this email for your reference.\n\nNeed help? Reply to this email.");
  return text.toString();
 }
 private static String rupees(int amount){return String.format(Locale.ROOT,"INR %,d",amount);}
}
