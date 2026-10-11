package com.styledsomehow.backend.payments;
import java.util.List;
public interface PaymentGateway {
 record GatewayOrder(String id,long amount,String currency,String receipt){}
 record Payment(String id,String orderId,long amount,String currency,String status,long refundedAmount){}
 record Refund(String id,String status){}
 boolean ready();String keyId();
 boolean verifyCheckout(String orderId,String paymentId,String signature);
 boolean verifyWebhook(byte[] body,String signature);
 GatewayOrder createOrFindOrder(String receipt,long amount);
 Payment fetchPayment(String id);
 List<Payment> payments(String orderId);
 Refund refund(String paymentId,long amount,String idempotencyKey);
 Refund fetchRefund(String id);
}
