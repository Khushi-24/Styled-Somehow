package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.PendingOrder;
import java.util.Locale;
/** Email-client compatible table layout; all customer content is escaped. */
public final class OrderEmailTemplate {
 private OrderEmailTemplate(){}
 static String escape(String value){return value==null?"":value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
 private static String money(int amount){return String.format(Locale.ROOT,"&#8377;%,d",amount);}
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
   +"</td></tr><tr><td style='padding:20px 24px;border-top:1px solid #eee;font-size:12px;color:#777;line-height:1.7'>Need help? Email <a href='mailto:contact@styledsomehow.in' style='color:#555'>contact@styledsomehow.in</a>.</td></tr></table>"
   +"<p style='font-size:11px;color:#888'>If the button doesn’t work, open this link:<br><a href='"+link+"' style='color:#666;word-break:break-all'>"+link+"</a></p></td></tr></table></body></html>";
 }
}
