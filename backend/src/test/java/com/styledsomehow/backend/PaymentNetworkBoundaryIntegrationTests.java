package com.styledsomehow.backend;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.inventory.*;
import com.styledsomehow.backend.operations.*;
import com.styledsomehow.backend.payments.*;
import com.styledsomehow.backend.address.PostalLocationService;
import com.styledsomehow.backend.shipping.ShippingGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest
class PaymentNetworkBoundaryIntegrationTests {
 @Autowired CheckoutService checkout;@Autowired PaymentService payments;@Autowired OrderRepository orders;@Autowired StockRepository stocks;@Autowired PaymentRepository receipts;@Autowired MovementRepository movements;@Autowired OrderOperationsController operations;@Autowired PlatformTransactionManager manager;
 @MockitoBean PaymentGateway gateway;@MockitoBean ShippingGateway shipping;@MockitoBean PostalLocationService locations;
 @Test void providerCreationVerificationAndRefundRunOutsideDatabaseTransactions(){
  when(shipping.rate(anyString(),anyInt(),anyInt())).thenReturn(new ShippingGateway.Rate(1,"Delhivery",0));when(gateway.ready()).thenReturn(true);when(gateway.keyId()).thenReturn("rzp_test_fixture");when(gateway.verifyCheckout(anyString(),anyString(),anyString())).thenReturn(true);
  when(gateway.createOrFindOrder(anyString(),anyLong())).thenAnswer(call->{assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();return new PaymentGateway.GatewayOrder("order_boundary",call.getArgument(1),"INR",call.getArgument(0));});
  var stock=stocks.findAll().stream().filter(s->s.colour.equals("White")&&s.size.equals("S")).findFirst().orElseThrow();int quantity=stock.quantity;int reserved=stock.reserved;String owner="boundary-"+UUID.randomUUID();
  var o=checkout.create(new CheckoutDtos.Checkout(List.of(new CheckoutDtos.Item("cherry-zest-oversized-t-shirt","White","S",1)),new CheckoutDtos.Address("Fixture","boundary@gmail.com","9999999999","Test address","","Ahmedabad","Gujarat","380015"),UUID.randomUUID().toString(),599),owner);
  try{
   payments.start(o.id,owner);
   when(gateway.fetchPayment("pay_boundary")).thenAnswer(call->{assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();return new PaymentGateway.Payment("pay_boundary","order_boundary",59900,"INR","captured",0);});
   assertThat(payments.verify(o.id,owner,"pay_boundary","fixture").status).isEqualTo("PAID");
   operations.refund(o.id,new OrderOperationsController.RefundRequest("Approved fixture cancellation",false),()->"owner");
   when(gateway.refund(eq("pay_boundary"),eq(59900L),anyString())).thenAnswer(call->{assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();return new PaymentGateway.Refund("rfnd_boundary","processed");});
   payments.reconcileRefund("pay_boundary");assertThat(orders.findById(o.id).orElseThrow().status).isEqualTo("REFUNDED");
  }finally{new TransactionTemplate(manager).executeWithoutResult(status->{checkout.cancel(o.id,owner);receipts.findAll().stream().filter(r->o.id.equals(r.orderId)).forEach(receipts::delete);receipts.flush();orders.deleteById(o.id);orders.flush();movements.findAll().stream().filter(m->m.reason!=null&&m.reason.endsWith(o.id)).forEach(movements::delete);var current=stocks.locked(stock.id).orElseThrow();current.quantity=quantity;current.reserved=reserved;stocks.saveAndFlush(current);});}
 }
}
