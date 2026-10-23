package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.PendingOrder;
import java.util.Locale;
/** Email-client compatible table layout; all customer content is escaped. */
public final class OrderEmailTemplate {
 private OrderEmailTemplate(){}
 static String escape(String value){return value==null?"":value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
 private static String money(int amount){return String.format(Locale.ROOT,"&#8377;%,d",amount);}
 static String noReplyText(){return "This is an automated email. Please do not reply. For help, send a new email to contact@styledsomehow.in.";}
 static String noReplyHtml(){return "<p style='font-size:11px;color:#777;line-height:1.8;margin:0'>This is an automated email. Please do not reply.<br>For help, send a new email to <a href='mailto:contact@styledsomehow.in?subject=Styled%20Somehow%20support' style='color:#555;text-decoration:underline'>contact@styledsomehow.in</a>.</p>";}
 private static String frame(String title,String preview,String content,String logo){
  return "<!doctype html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>"+escape(title)+"</title></head><body style='margin:0;padding:0;background:#f5f4f2;font-family:Arial,Helvetica,sans-serif;color:#161616'><div style='display:none;max-height:0;overflow:hidden'>"+escape(preview)+"</div><table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='border-collapse:collapse'><tr><td align='center' style='padding:24px 12px'><table role='presentation' width='560' cellspacing='0' cellpadding='0' style='width:100%;max-width:560px;background:#fff;border:1px solid #e4e1dd;border-radius:8px'><tr><td align='center' style='padding:24px;border-bottom:1px solid #eee'><img src='"+escape(logo)+"' width='120' alt='Styled Somehow' style='display:block;width:120px;max-width:100%;height:auto'></td></tr><tr><td style='padding:28px 24px'>"+content+"</td></tr><tr><td style='padding:20px 24px;border-top:1px solid #eee'>"+noReplyHtml()+"</td></tr></table></td></tr></table></body></html>";
 }
 static String otp(String code,String logo){
  if(code==null||!code.matches("[0-9]{6}"))throw new IllegalArgumentException("Invalid login code");
  String body="<h1 style='font-size:25px;font-weight:700;line-height:1.3;letter-spacing:-.5px;text-align:center;margin:0 0 12px'>Your login code</h1><p style='font-size:14px;line-height:1.7;text-align:center;color:#666;margin:0 0 24px'>Enter this code to log in to Styled Somehow.</p><table role='presentation' width='100%' cellspacing='0' cellpadding='0'><tr><td align='center' style='background:#f6f5f4;border:1px solid #e6e3e0;border-radius:6px;padding:20px 12px;font-family:Consolas,monospace;font-size:32px;font-weight:700;letter-spacing:6px;color:#161616'>"+code+"</td></tr></table><p style='text-align:center;font-size:12px;color:#666;line-height:1.8;margin:18px 0 0'>Valid for 10 minutes. Do not share this code.</p><p style='text-align:center;font-size:11px;color:#888;line-height:1.8;margin:16px 0 0'>Didn’t request this? You can ignore this email.</p>";
  return frame("Your login code","Your Styled Somehow login code. Valid for 10 minutes.",body,logo);
 }
 static String status(PendingOrder order,String stage,String link,String logo){
  String label=SmtpEmailDelivery.statusLabel(stage),description=SmtpEmailDelivery.statusDescription(order,stage);
  String test="TEST".equals(order.paymentMode)?"<p style='background:#fff5d8;border-radius:4px;padding:10px 12px;font-size:11px;color:#705018;line-height:1.7;margin:18px 0'>TEST ORDER &middot; This order will not be dispatched.</p>":"";
  String body="<h1 style='font-size:26px;line-height:1.3;font-weight:700;letter-spacing:-.5px;margin:0 0 10px'>"+escape(label)+"</h1><p style='font-size:14px;line-height:1.7;color:#666;margin:0 0 22px'>"+escape(description)+"</p>"+test+"<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='background:#f7f6f5;border:1px solid #e9e6e3;border-radius:5px'><tr><td style='padding:14px 16px'><span style='font-size:10px;color:#777;letter-spacing:1px'>ORDER REFERENCE</span><br><strong style='font-size:12px;font-weight:500;word-break:break-all;line-height:1.8'>"+escape(order.id)+"</strong></td></tr></table><table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='margin-top:24px'><tr><td align='center' bgcolor='#161616' style='border-radius:5px'><a href='"+escape(link)+"' style='display:block;padding:14px 20px;color:#fff;text-decoration:none;font-size:14px;font-weight:700;line-height:1.5'>View order</a></td></tr></table><p style='font-size:11px;color:#888;line-height:1.7;text-align:center;margin:14px 0 0'>Keep your order link private.</p>";
  return frame(label,description,body,logo);
 }
 static String recovery(String text,String logo){return frame("Your order references","Your Styled Somehow order references","<h1 style='font-size:24px;margin:0 0 18px'>Your order references</h1><p style='font-size:13px;line-height:1.8;white-space:pre-line'>"+escape(text)+"</p>",logo);}
 public static String html(PendingOrder o,String orderLink,String logoUrl){
  String link=escape(orderLink);
  String greeting=o.name==null||o.name.isBlank()?"Thank you!":"Thank you, "+escape(o.name.trim().split("\\s+")[0])+"!";
  var rows=new StringBuilder();
  for(var line:o.items)rows.append("<tr><td style='padding:14px 0;border-bottom:1px solid #eee'><strong>").append(escape(line.name)).append("</strong><br><span style='font-size:12px;color:#666'>").append(escape(line.colour)).append(" &middot; ").append(escape(line.size)).append(" &middot; Qty ").append(line.quantity).append("</span></td><td align='right' style='padding:14px 0;border-bottom:1px solid #eee;white-space:nowrap'>").append(money(line.unitPrice*line.quantity)).append("</td></tr>");
  String test="TEST".equals(o.paymentMode)?"<p style='background:#fff5d8;padding:12px;font-size:12px;color:#705018'>TEST ORDER &middot; No real money charged. This order will not be dispatched.</p>":"";
  return "<!doctype html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>Order confirmed</title></head>"
   +"<body style='margin:0;background:#f5f4f2;font-family:Arial,Helvetica,sans-serif;color:#161616'><div style='display:none;max-height:0;overflow:hidden'>Your order is confirmed. Keep this email to view your order status.</div>"
   +"<table role='presentation' width='100%' cellspacing='0' cellpadding='0'><tr><td align='center' style='padding:24px 12px'><table role='presentation' width='600' cellspacing='0' cellpadding='0' style='width:100%;max-width:600px;background:#fff;border:1px solid #e4e1dd;border-radius:10px'>"
   +"<tr><td align='center' style='padding:26px 24px;border-bottom:1px solid #eee'><img src='"+escape(logoUrl)+"' width='130' alt='Styled Somehow' style='display:block;width:130px;max-width:100%;height:auto'></td></tr>"
   +"<tr><td style='padding:28px 24px'><p style='font-size:11px;letter-spacing:2px;font-weight:700;color:#287140;margin:0 0 12px'>ORDER CONFIRMED</p>"
   +"<h1 style='font-size:28px;line-height:1.2;letter-spacing:-1px;margin:0 0 10px'>"+greeting+"</h1><p style='font-size:14px;line-height:1.6;margin:0 0 22px'>Your order is confirmed.</p>"+test
   +"<div style='background:#f6f5f4;padding:14px;border-radius:6px'><span style='font-size:10px;color:#666;letter-spacing:1px'>ORDER REFERENCE</span><br><strong style='display:block;font-size:13px;line-height:1.6;word-break:break-all;margin-top:5px'>"+escape(o.id)+"</strong></div>"
   +"<table width='100%' cellspacing='0' cellpadding='0' style='font-size:13px;line-height:1.6;margin-top:24px'><tr><th align='left' style='padding:10px 0;border-bottom:1px solid #ddd;color:#666;font-weight:500'>Item</th><th align='right' style='padding:10px 0;border-bottom:1px solid #ddd;color:#666;font-weight:500'>Price</th></tr>"+rows
   +"<tr><td style='padding-top:16px;color:#666'>Subtotal</td><td align='right' style='padding-top:16px'>"+money(o.subtotal)+"</td></tr>"
   +"<tr><td style='padding:8px 0;color:#666'>Shipping</td><td align='right'>"+(o.shipping==0?"Free":money(o.shipping))+"</td></tr>"
   +"<tr><td style='padding:14px 0;border-top:1px solid #ddd;font-size:17px'><strong>Total paid</strong></td><td align='right' style='padding:14px 0;border-top:1px solid #ddd;font-size:17px'><strong>"+money(o.total)+"</strong></td></tr></table>"
   +"<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='margin-top:16px'><tr><td align='center' bgcolor='#161616' style='border-radius:5px'><a href='"+link+"' style='display:block;padding:15px 24px;color:#fff;text-decoration:none;font-size:14px;font-weight:bold'>View order</a></td></tr></table>"
   +"<p style='font-size:12px;color:#777;line-height:1.7;text-align:center;margin:12px 0 0'>This private link opens your order without a code. Keep it safe and don’t share it.</p>"
   +"</td></tr><tr><td style='padding:20px 24px;border-top:1px solid #eee'>"+noReplyHtml()+"</td></tr></table>"
   +"<p style='font-size:11px;color:#888'>If the button doesn’t work, open this link:<br><a href='"+link+"' style='color:#666;word-break:break-all'>"+link+"</a></p></td></tr></table></body></html>";
 }
}
