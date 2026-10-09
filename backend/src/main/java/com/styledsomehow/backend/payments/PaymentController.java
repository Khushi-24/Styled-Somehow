package com.styledsomehow.backend.payments;
import com.styledsomehow.backend.checkout.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
@RestController
public class PaymentController {
 private final PaymentService payments;private final PaymentGateway gateway;private final ObjectMapper json;
 public PaymentController(PaymentService payments,PaymentGateway gateway,ObjectMapper json){this.payments=payments;this.gateway=gateway;this.json=json;}
 public record Verification(@NotNull @Pattern(regexp="pay_[A-Za-z0-9]+") String paymentId,@NotNull @Pattern(regexp="[a-fA-F0-9]{64}") String signature){}
 @GetMapping("/api/payments/config") public Map<String,Object> config(){return Map.of("enabled",gateway.ready(),"mode","TEST");}
 @PostMapping("/api/checkout/{id}/payment") public PaymentService.Start start(@PathVariable String id,HttpSession session){return payments.start(id,CheckoutController.key(session));}
 @PostMapping("/api/checkout/{id}/payment/verify") public PendingOrder verify(@PathVariable String id,@Valid @RequestBody Verification body,HttpSession session){return payments.verify(id,CheckoutController.key(session),body.paymentId(),body.signature());}
 @PostMapping("/api/checkout/{id}/payment/check") public PendingOrder refresh(@PathVariable String id,HttpSession session){return payments.refresh(id,CheckoutController.key(session));}
 @PostMapping("/api/payments/razorpay/webhook") @ResponseStatus(HttpStatus.NO_CONTENT) public void webhook(@RequestBody byte[] body,@RequestHeader(value="X-Razorpay-Signature",required=false) String signature){
  if(!gateway.ready()||!gateway.verifyWebhook(body,signature))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid webhook signature");
  if(body.length>262144)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
  try{var event=json.readTree(body);String name=event.path("event").asText();if(!List.of("payment.captured","payment.authorized","payment.failed","order.paid","refund.processed","refund.failed").contains(name))return;
   String id=event.path("payload").path("payment").path("entity").path("id").asText();if(id.isBlank())id=event.path("payload").path("refund").path("entity").path("payment_id").asText();if(!id.matches("pay_[A-Za-z0-9]+"))throw new IllegalArgumentException();payments.webhook(id);
  }catch(ResponseStatusException e){throw e;}catch(tools.jackson.core.JacksonException|IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid webhook payload");}
 }
 @GetMapping("/api/admin/payments") public List<PaymentReceipt> receipts(){return payments.adminReceipts();}
}
