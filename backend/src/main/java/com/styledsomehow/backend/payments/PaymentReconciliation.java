package com.styledsomehow.backend.payments;
import com.styledsomehow.backend.checkout.OrderRepository;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.List;
@Component
public class PaymentReconciliation {
 private final PaymentGateway gateway;private final PaymentService payments;private final OrderRepository orders;private final PaymentRepository receipts;
 public PaymentReconciliation(PaymentService payments,OrderRepository orders,PaymentRepository receipts,PaymentGateway gateway){this.gateway=gateway;this.payments=payments;this.orders=orders;this.receipts=receipts;}
 @Scheduled(fixedDelay=60000,initialDelay=60000) public void run(){
  if(!gateway.ready())return;
  for(var o:orders.paymentChecks(org.springframework.data.domain.PageRequest.of(0,20)))try{payments.reconcileOrder(o.id);}catch(RuntimeException e){/* Retry without logging private payloads. */}finally{payments.markChecked(o.id);}
  for(var r:receipts.findTop20ByStatusInOrderByUpdatedAtAsc(List.of("REFUND_PENDING","REFUND_REQUESTED")))try{payments.reconcileRefund(r.id);}catch(RuntimeException e){/* Durable queue is retained. */}
 }
}
