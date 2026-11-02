package com.styledsomehow.backend;
import com.styledsomehow.backend.operations.*;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.inventory.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @Transactional
class CommerceControlsIntegrationTests {
 @Autowired CommerceGuard guard;@Autowired CommerceControlRepository controls;@Autowired InventoryService inventory;@Autowired CheckoutService checkout;
 @Test void freshBrowsersAndEmailsCannotBypassIpAllowance(){for(int i=0;i<20;i++)guard.check("checkout","fixture-ip","browser-"+i,"person"+i+"@gmail.com");assertThatThrownBy(()->guard.check("checkout","fixture-ip","another-browser","different@gmail.com")).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(429));guard.check("checkout","another-ip","another-browser","different@gmail.com");}
 @Test void pauseBlocksNewCheckoutsButLeavesCatalogueQuotesAvailable(){var c=controls.lock();c.ordersPaused=true;controls.saveAndFlush(c);assertThatThrownBy(guard::requireOpen).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(503));assertThat(checkout.quote(new CheckoutDtos.Cart(List.of(new CheckoutDtos.Item("cherry-zest-oversized-t-shirt","White","S",1)))).subtotal()).isEqualTo(599);}
 @Test void newColourStartsEmptyAndUsesAuditedAdjustments(){var stock=inventory.create(new InventoryService.NewVariant("Beige","S"));assertThat(stock.quantity).isZero();assertThat(stock.reserved).isZero();inventory.adjust(stock.id,new InventoryService.Adjustment(stock.version,12,"Opening beige stock",UUID.randomUUID().toString()),"owner");assertThat(inventory.availability().get("Beige").get("S")).isTrue();assertThatThrownBy(()->inventory.create(new InventoryService.NewVariant("beige","S"))).isInstanceOf(ResponseStatusException.class);}
}
