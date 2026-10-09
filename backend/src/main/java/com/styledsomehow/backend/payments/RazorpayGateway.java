package com.styledsomehow.backend.payments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.nio.charset.StandardCharsets;
@Component
public class RazorpayGateway implements PaymentGateway {
 private final String key,secret,webhook;private final RestClient client;
 public RazorpayGateway(@Value("${payments.razorpay.key-id:}") String key,@Value("${payments.razorpay.key-secret:}") String secret,@Value("${payments.razorpay.webhook-secret:}") String webhook){
  this.key=key;this.secret=secret;this.webhook=webhook;
  var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());factory.setReadTimeout(Duration.ofSeconds(10));
  client=RestClient.builder().requestFactory(factory).baseUrl("https://api.razorpay.com/v1").defaultHeaders(h->h.setBasicAuth(key,secret)).build();
 }
 // This release deliberately accepts test credentials only.
 public boolean ready(){return key.matches("rzp_test_[A-Za-z0-9]+")&&!secret.isBlank()&&webhook.length()>=32;}
 public String keyId(){return key;}
 public static boolean validSignature(byte[] body,String signature,String secret){
  if(secret.isBlank()||signature==null||!signature.matches("[a-fA-F0-9]{64}"))return false;
  try{var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return java.security.MessageDigest.isEqual(mac.doFinal(body),HexFormat.of().parseHex(signature));}catch(java.security.GeneralSecurityException e){throw new IllegalStateException(e);}
 }
 public boolean verifyCheckout(String orderId,String paymentId,String signature){return validSignature((orderId+"|"+paymentId).getBytes(StandardCharsets.UTF_8),signature,secret);}
 public boolean verifyWebhook(byte[] body,String signature){return validSignature(body,signature,webhook);}
 private JsonNode get(String uri){try{return Objects.requireNonNull(client.get().uri(uri).retrieve().body(JsonNode.class));}catch(RuntimeException e){throw unavailable();}}
 private JsonNode post(String uri,Map<String,Object> body,String idempotency){try{var request=client.post().uri(uri).contentType(org.springframework.http.MediaType.APPLICATION_JSON);if(idempotency!=null)request.header("X-Refund-Idempotency",idempotency);return Objects.requireNonNull(request.body(body).retrieve().body(JsonNode.class));}catch(RuntimeException e){throw unavailable();}}
 private ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Payment service temporarily unavailable. Please retry.");}
 private GatewayOrder order(JsonNode n){return new GatewayOrder(n.path("id").asText(),n.path("amount").asLong(),n.path("currency").asText(),n.path("receipt").asText());}
 private Payment payment(JsonNode n){return new Payment(n.path("id").asText(),n.path("order_id").asText(),n.path("amount").asLong(),n.path("currency").asText(),n.path("status").asText(),n.path("amount_refunded").asLong());}
 private Refund refund(JsonNode n){return new Refund(n.path("id").asText(),n.path("status").asText());}
 public GatewayOrder createOrFindOrder(String receipt,long amount){
  var items=get("/orders?receipt="+receipt).path("items");if(items.size()>1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment reference needs review. Please contact the store.");
  return order(items.size()==1?items.get(0):post("/orders",Map.of("amount",amount,"currency","INR","receipt",receipt,"partial_payment",false),null));
 }
 public Payment fetchPayment(String id){return payment(get("/payments/"+id));}
 public List<Payment> payments(String orderId){var result=new ArrayList<Payment>();for(var n:get("/orders/"+orderId+"/payments").path("items"))result.add(payment(n));return result;}
 public Refund refund(String paymentId,long amount,String idempotency){return refund(post("/payments/"+paymentId+"/refund",Map.of("amount",amount,"speed","normal"),idempotency));}
 public Refund fetchRefund(String id){return refund(get("/refunds/"+id));}
}
