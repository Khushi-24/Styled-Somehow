package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.fulfilment.FulfilmentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api/order-access/login")
public class EmailLoginController {
 private final EmailLoginService login;private final OrderRepository orders;
 public EmailLoginController(EmailLoginService login,OrderRepository orders){this.login=login;this.orders=orders;}
 public record EmailRequest(@NotBlank @Email @Size(max=254) String email){}
 public record VerifyRequest(@NotBlank @Pattern(regexp="[a-fA-F0-9-]{36}") String challengeId,@NotBlank @Pattern(regexp="[0-9]{6}") String code){}
 public record Session(String email){}
 public record Summary(String id,Instant createdAt,String status,String stage,String paymentMode,int total,int quantity,String itemName){}
 public record History(String email,List<Summary> orders,boolean hasMore){}
 @PostMapping("/code") public ResponseEntity<EmailLoginService.CodeRequest> request(@Valid @RequestBody EmailRequest body,HttpServletRequest request){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(login.request(body.email(),CheckoutController.key(request.getSession()),request.getRemoteAddr()));}
 @PostMapping("/verify") public ResponseEntity<Session> verify(@Valid @RequestBody VerifyRequest body,HttpServletRequest request){
  String email=login.verify(body.challengeId(),body.code(),CheckoutController.key(request.getSession())).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Code is incorrect, expired or has too many attempts. Request a new code if needed."));
  CustomerLoginSession.grant(email,request);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Session(email));
 }
 static String email(HttpSession session){Object until=session.getAttribute("customer-until"),email=session.getAttribute("customer-email");if(!(until instanceof Instant time)||!time.isAfter(Instant.now())||!(email instanceof String value))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Log in to see your orders.");return value;}
 static boolean owns(PendingOrder order,HttpSession session){try{return order.paidAt!=null&&order.paymentId!=null&&email(session).equalsIgnoreCase(order.email);}catch(ResponseStatusException expired){return false;}}
 @GetMapping("/session") public ResponseEntity<Session> session(HttpSession session){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Session(email(session)));}
 @Transactional(readOnly=true) @GetMapping("/orders") public ResponseEntity<History> history(@RequestParam(defaultValue="0") int page,HttpSession session){
  String email=email(session);if(page<0||page>10000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  var result=orders.emailHistory(email,PageRequest.of(page,20));return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new History(email,result.getContent().stream().map(o->new Summary(o.id,o.createdAt,o.status,FulfilmentService.stage(o),o.paymentMode,o.total,o.items.stream().mapToInt(i->i.quantity).sum(),o.items.isEmpty()?"T-shirt":o.items.get(0).name)).toList(),result.hasNext()));
 }
 @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) public void logout(HttpSession session){session.removeAttribute("customer-email");session.removeAttribute("customer-until");session.removeAttribute("verified-order-id");session.removeAttribute("verified-order-until");}
}
