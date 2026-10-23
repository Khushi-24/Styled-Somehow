package com.styledsomehow.backend;
import com.styledsomehow.backend.operations.*;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.payments.*;
import com.styledsomehow.backend.inventory.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @Transactional
class RefundOperationsIntegrationTests {
 @Autowired OrderOperationsController operations;@Autowired OrderRepository orders;@Autowired PaymentRepository payments;@Autowired StockRepository stock;
 private PendingOrder paid(String stage){var o=new PendingOrder();o.id=UUID.randomUUID().toString();o.status="PAID";o.paymentMode="TEST";o.paymentId="pay_"+UUID.randomUUID().toString().replace("-","");o.fulfilmentStage=stage;o.total=599;o.subtotal=599;o.paidAt=Instant.now();o.createdAt=o.paidAt;o.expiresAt=o.paidAt;o=orders.saveAndFlush(o);var r=new PaymentReceipt();r.id=o.paymentId;r.orderId=o.id;r.amount=59900;r.status="PAID";r.refundKey=UUID.randomUUID().toString();r.createdAt=o.paidAt;r.updatedAt=o.paidAt;payments.saveAndFlush(r);return o;}
 @Test void approvedFullRefundIsIdempotentAndDoesNotCreateBlankStock(){var o=paid("PACKING");int before=stock.findAll().stream().mapToInt(s->s.quantity).sum();var input=new OrderOperationsController.RefundRequest("Customer cancellation approved",false);operations.refund(o.id,input,()->"owner");operations.refund(o.id,input,()->"owner");assertThat(o.status).isEqualTo("REFUND_PENDING");assertThat(payments.findById(o.paymentId).orElseThrow().status).isEqualTo("REFUND_PENDING");assertThat(stock.findAll().stream().mapToInt(s->s.quantity).sum()).isEqualTo(before);}
 @Test void deliveredOrderRequiresInspectedReturn(){var o=paid("DELIVERED");assertThatThrownBy(()->operations.refund(o.id,new OrderOperationsController.RefundRequest("Approved return",false),()->"owner")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);operations.refund(o.id,new OrderOperationsController.RefundRequest("Approved inspected return",true),()->"owner");assertThat(o.status).isEqualTo("REFUND_PENDING");}
}
