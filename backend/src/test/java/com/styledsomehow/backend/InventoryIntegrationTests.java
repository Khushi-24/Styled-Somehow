package com.styledsomehow.backend;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.styledsomehow.backend.inventory.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @Transactional
class InventoryIntegrationTests {
 @Autowired InventoryService inventory;
 @Autowired MockMvc mvc;
 @Test void confirmedOpeningStockAndSharedAvailability()throws Exception{
  var stocks=inventory.list();assertThat(stocks).hasSize(8);assertThat(stocks.stream().mapToInt(s->s.quantity).sum()).isEqualTo(72);
  assertThat(stocks).allSatisfy(s->assertThat(s.quantity).isEqualTo(s.size.equals("XL")?0:12));
  mvc.perform(get("/api/admin/inventory")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/products/cherry-zest-oversized-t-shirt")).andExpect(jsonPath("$.availability.White.XL").value(false)).andExpect(jsonPath("$.availability.White.S").value(true));
 }
 @Test void adjustmentLedgerIdempotencyAndSharedZero()throws Exception{
  var stock=inventory.list().stream().filter(s->s.colour.equals("Black")&&s.size.equals("M")).findFirst().orElseThrow();
  var a=new InventoryService.Adjustment(stock.version,-12,"Damaged blanks",java.util.UUID.randomUUID().toString());
  var changed=inventory.adjust(stock.id,a,"test-owner");assertThat(changed.quantity).isZero();
  assertThat(inventory.adjust(stock.id,a,"test-owner").quantity).isZero();
  assertThat(inventory.history().stream().filter(m->a.requestId().equals(m.requestId)).count()).isEqualTo(1);
  assertThat(inventory.availability().get("Black").get("M")).isFalse();
  mvc.perform(get("/api/products/shes-winning-oversized-t-shirt")).andExpect(jsonPath("$.availability.Black.M").value(false));
  mvc.perform(get("/api/products/chilli-crush-oversized-t-shirt")).andExpect(jsonPath("$.availability.Black.M").value(false));
  var invalid=new InventoryService.Adjustment(changed.version,-1,"Remove",java.util.UUID.randomUUID().toString());
  assertThatThrownBy(()->inventory.adjust(stock.id,invalid,"test-owner")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }
}
