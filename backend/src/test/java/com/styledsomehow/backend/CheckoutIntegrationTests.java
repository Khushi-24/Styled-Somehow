package com.styledsomehow.backend;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.inventory.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.Instant;
import static com.styledsomehow.backend.checkout.CheckoutDtos.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @Transactional
class CheckoutIntegrationTests {
 @Autowired CheckoutService checkout;
 @Autowired StockRepository stocks;
 @Autowired OrderRepository orders;
 private Address address(){return new Address("Integration fixture","fixture@example.test","9999999999","Test address","","Ahmedabad","Gujarat","380054");}
 private Checkout request(String key){return new Checkout(List.of(new Item("cherry-zest-oversized-t-shirt","White","S",2)),address(),key,1198);}
 @Test void authoritativePriceAndFreeShipping(){var q=checkout.quote(new Cart(request(UUID.randomUUID().toString()).items()));assertThat(q.subtotal()).isEqualTo(1198);assertThat(q.shipping()).isZero();assertThat(q.total()).isEqualTo(1198);}
 @Test void combinedDesignsCannotExceedSharedBlanks(){assertThatThrownBy(()->checkout.quote(new Cart(List.of(new Item("cherry-zest-oversized-t-shirt","White","M",7),new Item("untamed-torque-oversized-t-shirt","White","M",6))))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);}
 @Test void reserveIdempotencyCancelAndExpiry(){
  var request=request(UUID.randomUUID().toString());var order=checkout.create(request,"fixture-session");assertThat(order.status).isEqualTo("PENDING_PAYMENT");
  var s=stocks.findAll().stream().filter(v->v.colour.equals("White")&&v.size.equals("S")).findFirst().orElseThrow();assertThat(s.quantity).isEqualTo(12);assertThat(s.reserved).isEqualTo(2);
  assertThat(checkout.create(request,"fixture-session").id).isEqualTo(order.id);assertThat(s.reserved).isEqualTo(2);
  checkout.cancel(order.id,"fixture-session");assertThat(s.reserved).isZero();assertThat(order.status).isEqualTo("CANCELLED");
  var second=checkout.create(request(UUID.randomUUID().toString()),"fixture-session");second.expiresAt=Instant.now().minusSeconds(1);orders.saveAndFlush(second);checkout.expireReservations();assertThat(second.status).isEqualTo("EXPIRED");assertThat(s.reserved).isZero();
 }
 @Test void cannotTrustClientTotalOrAccessAnotherGuestsOrder(){
  var req=request(UUID.randomUUID().toString());var order=checkout.create(req,"owner");assertThatThrownBy(()->checkout.get(order.id,"other-browser")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }
 @Test void rejectsChangedTotal(){var req=request(UUID.randomUUID().toString());assertThatThrownBy(()->checkout.create(new Checkout(req.items(),address(),req.requestId(),1),"price-fixture")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);}
}
