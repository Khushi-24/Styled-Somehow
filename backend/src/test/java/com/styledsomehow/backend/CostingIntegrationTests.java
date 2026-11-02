package com.styledsomehow.backend;
import com.styledsomehow.backend.costing.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import static com.styledsomehow.backend.costing.CostingDtos.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @AutoConfigureMockMvc @Transactional
class CostingIntegrationTests {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;
 private Profile profile(){return new Profile(-1,Arrays.stream(Key.values()).map(k->new Entry(k,new BigDecimal("10.50"),true)).toList(),BigDecimal.ZERO,false);}
 private String savePath(){return "/api/admin/costing/products/1?month=2026-10";}
 private Dashboard dashboard()throws Exception{return json.readValue(mvc.perform(get("/api/admin/costing?month=2026-10").with(user("owner").roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),Dashboard.class);}
 @Test void adminOnlyAndCsrfProtected()throws Exception{
  mvc.perform(get("/api/admin/costing?month=2026-10")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/admin/costing?month=2026-10").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
  mvc.perform(put(savePath()).with(user("owner").roles("ADMIN")).contentType("application/json").content(json.writeValueAsString(profile()))).andExpect(status().isForbidden());
  mvc.perform(put("/api/admin/costing/months/2026-10").with(user("owner").roles("ADMIN")).contentType("application/json").content("{}")).andExpect(status().isForbidden());
 }
 @Test void savedProfilesArePrivatePerProductAndRejectStaleEdits()throws Exception{
  var fresh=dashboard();assertThat(fresh.fixedTshirtCost()).isEqualByComparingTo("280");assertThat(fresh.products().getFirst().profile().items()).allMatch(Entry::included);assertThat(fresh.products()).allMatch(p->!p.estimate().complete());
  var input=profile();String body=mvc.perform(put(savePath()).with(user("owner").roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();var row=json.readValue(body,Row.class);
  assertThat(row.profile().version()).isGreaterThanOrEqualTo(0);assertThat(row.estimate().profit()).isEqualByComparingTo("224.50");
  assertThat(dashboard().products().stream().filter(p->p.productId()!=1)).allMatch(p->p.profile().version()==-1);
  mvc.perform(put(savePath()).with(user("owner").roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isConflict());
  String publicBody=mvc.perform(get("/api/products/cherry-zest-oversized-t-shirt")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertThat(publicBody).doesNotContain("customerShipping","allocateMonthly","DTF_PRINT","costing");
  var items=new ArrayList<>(row.profile().items());items.set(0,new Entry(Key.OUTER_PACKAGING,new BigDecimal("10.50"),false));var changed=new Profile(row.profile().version(),items,BigDecimal.ZERO,false);
  mvc.perform(put(savePath()).with(user("owner").roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(changed))).andExpect(status().isOk()).andExpect(jsonPath("$.estimate.profit").value(235));
  var saved=dashboard().products().stream().filter(p->p.productId()==1).findFirst().orElseThrow();assertThat(saved.profile().items().getFirst().included()).isFalse();assertThat(saved.profile().items().getFirst().amount()).isEqualByComparingTo("10.50");
 }
 @Test void monthlyBudgetsPersistSeparatelyAndValidateAmountsAndMonths()throws Exception{
  var budget=new Month(-1,new BigDecimal("1000"),new BigDecimal("400"),new BigDecimal("2000"),BigDecimal.ZERO,100);
  mvc.perform(put("/api/admin/costing/months/2026-10").with(user("owner").roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(budget))).andExpect(status().isOk());
  assertThat(dashboard().budget().ai()).isEqualByComparingTo("2000");
  var p=profile();p=new Profile(-1,p.items(),BigDecimal.ZERO,true);mvc.perform(put(savePath()).with(user("owner").roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(p))).andExpect(status().isOk()).andExpect(jsonPath("$.estimate.overhead").value(34));
  mvc.perform(put("/api/admin/costing/months/2026-10").with(user("owner").roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(budget))).andExpect(status().isConflict());
  mvc.perform(get("/api/admin/costing?month=2026-99").with(user("owner").roles("ADMIN"))).andExpect(status().isBadRequest());
  var negative=new ArrayList<>(profile().items());negative.set(0,new Entry(Key.OUTER_PACKAGING,new BigDecimal("-1"),true));mvc.perform(put(savePath()).with(user("owner").roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(new Profile(-1,negative,BigDecimal.ZERO,false)))).andExpect(status().isBadRequest());
  var duplicate=Collections.nCopies(9,new Entry(Key.SHIPPING,BigDecimal.ZERO,true));mvc.perform(put(savePath()).with(user("owner").roles("ADMIN")).with(csrf()).contentType("application/json").content(json.writeValueAsString(new Profile(-1,duplicate,BigDecimal.ZERO,false)))).andExpect(status().isBadRequest());
 }
}
