package com.styledsomehow.backend.checkout;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import static com.styledsomehow.backend.checkout.CheckoutDtos.*;
@RestController
public class CheckoutController {
 private final CheckoutService checkout;
 @org.springframework.beans.factory.annotation.Value("${email.confirmations.enabled:false}") private boolean confirmationsEnabled;
 public CheckoutController(CheckoutService checkout){this.checkout=checkout;}
 @PostMapping("/api/cart/quote") public Quote quote(@Valid @RequestBody Cart cart,HttpSession session){key(session);return checkout.quote(cart);}
 @PostMapping("/api/checkout/shipping-quote") public Quote shippingQuote(@Valid @RequestBody ShippingCart cart,HttpSession session){key(session);return checkout.shippingQuote(cart);}
 @PostMapping("/api/checkout") public PendingOrder create(@Valid @RequestBody Checkout request,HttpSession session){return checkout.create(request,key(session));}
 @GetMapping("/api/checkout/{id}") public PendingOrder get(@PathVariable String id,HttpSession session){return checkout.get(id,key(session));}
 public record ConfirmationEmail(String status){}
 @GetMapping("/api/checkout/{id}/confirmation-email") public org.springframework.http.ResponseEntity<ConfirmationEmail> confirmationEmail(@PathVariable String id,HttpSession session){
  var order=checkout.get(id,key(session));
  String status=order.confirmationSentAt!=null&&order.confirmationAttempts>0?"SENT":confirmationsEnabled&&"PAID".equals(order.status)&&order.confirmationSentAt==null?"PENDING":"UNAVAILABLE";
  return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(new ConfirmationEmail(status));
 }
 @PostMapping("/api/checkout/{id}/cancel") public PendingOrder cancel(@PathVariable String id,HttpSession session){return checkout.cancel(id,key(session));}
 public static String key(HttpSession session){synchronized(session){var value=session.getAttribute("guest-checkout-key");if(value==null){value=java.util.UUID.randomUUID().toString();session.setAttribute("guest-checkout-key",value);}return CheckoutService.digest(value.toString());}}
 @GetMapping("/api/admin/orders") public List<PendingOrder> orders(){return checkout.adminOrders();}
}
