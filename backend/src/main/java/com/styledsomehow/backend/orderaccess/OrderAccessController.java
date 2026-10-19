package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api/order-access")
public class OrderAccessController {
 @org.springframework.beans.factory.annotation.Autowired private com.styledsomehow.backend.fulfilment.FulfilmentService fulfilment;
 private final OrderLinkService links;private final CheckoutService checkout;private final OrderRepository orders;
 public OrderAccessController(OrderLinkService links,CheckoutService checkout,OrderRepository orders){this.links=links;this.checkout=checkout;this.orders=orders;}
 public record LinkRequest(@NotBlank @Size(max=160) String token){}
 public record CheckoutRequest(@NotBlank @Pattern(regexp="[a-fA-F0-9-]{36}") String orderId){}
 private Map<String,String> grant(String id,HttpServletRequest servlet){var session=servlet.getSession();synchronized(session){servlet.changeSessionId();session.setAttribute("verified-order-id",id);session.setAttribute("verified-order-until",Instant.now().plusSeconds(1800));}return Map.of("orderId",id);}
 @PostMapping("/link") public ResponseEntity<Map<String,String>> open(@Valid @RequestBody LinkRequest request,HttpServletRequest servlet){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(grant(links.verify(request.token()),servlet));}
 // Confirmation-page access is limited to the original checkout session.
 @PostMapping("/checkout") public ResponseEntity<Map<String,String>> checkout(@Valid @RequestBody CheckoutRequest request,HttpServletRequest servlet){
  var order=checkout.get(request.orderId(),CheckoutController.key(servlet.getSession()));
  if(order.paidAt==null||order.paymentId==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(grant(order.id,servlet));
 }
 @PostMapping({"/request","/verify"}) public void retired(){throw new ResponseStatusException(HttpStatus.GONE,"Email code requests and self-service recovery are no longer available. Use your confirmation email or contact contact@styledsomehow.in.");}
 @GetMapping("/order/{id}") public ResponseEntity<PendingOrder> order(@PathVariable String id,HttpSession session){
  Object until=session.getAttribute("verified-order-until");boolean linked=id.equals(session.getAttribute("verified-order-id"))&&until instanceof Instant time&&time.isAfter(Instant.now());
  if(!linked)EmailLoginController.email(session);
  var found=orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  if(!linked&&!EmailLoginController.owns(found,session))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(found);
 }
 @GetMapping("/order/{id}/fulfilment") public ResponseEntity<com.styledsomehow.backend.fulfilment.FulfilmentService.View> tracking(@PathVariable String id,HttpSession session){order(id,session);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(fulfilment.customer(id));}
 @GetMapping("/current") public ResponseEntity<PendingOrder> current(HttpSession session){var id=session.getAttribute("verified-order-id");if(!(id instanceof String value))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return order(value,session);}
 @PostMapping("/close") @ResponseStatus(HttpStatus.NO_CONTENT) public void close(HttpSession session){session.removeAttribute("verified-order-id");session.removeAttribute("verified-order-until");}
}
