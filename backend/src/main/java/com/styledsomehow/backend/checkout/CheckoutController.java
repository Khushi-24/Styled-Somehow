package com.styledsomehow.backend.checkout;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import static com.styledsomehow.backend.checkout.CheckoutDtos.*;
@RestController
public class CheckoutController {
 private final CheckoutService checkout;
 public CheckoutController(CheckoutService checkout){this.checkout=checkout;}
 @PostMapping("/api/cart/quote") public Quote quote(@Valid @RequestBody Cart cart,HttpSession session){key(session);return checkout.quote(cart);}
 @PostMapping("/api/checkout") public PendingOrder create(@Valid @RequestBody Checkout request,HttpSession session){return checkout.create(request,key(session));}
 @GetMapping("/api/checkout/{id}") public PendingOrder get(@PathVariable String id,HttpSession session){return checkout.get(id,key(session));}
 @PostMapping("/api/checkout/{id}/cancel") public PendingOrder cancel(@PathVariable String id,HttpSession session){return checkout.cancel(id,key(session));}
 private String key(HttpSession session){synchronized(session){var value=session.getAttribute("guest-checkout-key");if(value==null){value=java.util.UUID.randomUUID().toString();session.setAttribute("guest-checkout-key",value);}return CheckoutService.digest(value.toString());}}
 @GetMapping("/api/admin/orders") public List<PendingOrder> orders(){return checkout.adminOrders();}
}
