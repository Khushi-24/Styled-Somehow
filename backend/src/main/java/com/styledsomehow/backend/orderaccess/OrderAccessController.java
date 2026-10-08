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
 private final OrderAccessService access;private final CheckoutService checkout;private final OrderRepository orders;
 public OrderAccessController(OrderAccessService access,CheckoutService checkout,OrderRepository orders){this.access=access;this.checkout=checkout;this.orders=orders;}
 public record Request(@NotBlank @Pattern(regexp="[a-fA-F0-9-]{36}") String orderId,@NotBlank @Email @Size(max=180) String email){}
 public record Verify(@NotBlank @Pattern(regexp="[a-fA-F0-9-]{36}") String challengeId,@NotBlank @Pattern(regexp="[0-9]{6}") String code){}
 private String browser(HttpSession session){synchronized(session){var key=session.getAttribute("order-access-browser");if(key==null){key=UUID.randomUUID().toString();session.setAttribute("order-access-browser",key);}return key.toString();}}
 @PostMapping("/request") public OrderAccessService.Result request(@Valid @RequestBody Request request,HttpServletRequest servlet){return access.request(request.orderId(),request.email(),browser(servlet.getSession()),servlet.getRemoteAddr());}
 @PostMapping("/verify") public Map<String,String> verify(@Valid @RequestBody Verify request,HttpSession session){String id=access.verify(request.challengeId(),request.code(),browser(session)).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid or expired code. Request a new code if needed."));session.setAttribute("verified-order-id",id);session.setAttribute("verified-order-until",Instant.now().plusSeconds(1800));return Map.of("orderId",id);}
 @GetMapping("/order/{id}") public ResponseEntity<PendingOrder> order(@PathVariable String id,HttpSession session){Object until=session.getAttribute("verified-order-until");if(!id.equals(session.getAttribute("verified-order-id"))||!(until instanceof Instant time)||!time.isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  checkout.expireReservations();return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)));
 }
 @PostMapping("/close") @ResponseStatus(HttpStatus.NO_CONTENT) public void close(HttpSession session){session.removeAttribute("verified-order-id");session.removeAttribute("verified-order-until");}
}
