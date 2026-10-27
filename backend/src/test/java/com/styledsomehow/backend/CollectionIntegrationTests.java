package com.styledsomehow.backend;
import com.styledsomehow.backend.catalog.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.*;
@SpringBootTest @AutoConfigureMockMvc @Transactional
class CollectionIntegrationTests {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 private void create(String slug,String name)throws Exception{
  mvc.perform(post("/api/admin/collections").with(user("owner").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("slug",slug,"name",name)))).andExpect(status().isCreated());
 }
 @Test void protectsAdminAndValidatesInput()throws Exception{
  mvc.perform(get("/api/collections")).andExpect(status().isOk()).andExpect(jsonPath("$[0].slug").value("women"));
  mvc.perform(get("/api/admin/collections")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/admin/collections").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
  mvc.perform(post("/api/admin/collections").with(user("owner").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{\"slug\":\"test\",\"name\":\"Test\"}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/admin/collections").with(user("owner").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"slug\":\"../bad\",\"name\":\"Bad\"}")).andExpect(status().isBadRequest());
  create("graphic-tees-fixture","Graphic Tees Fixture");
  mvc.perform(post("/api/admin/collections").with(user("owner").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"slug\":\"graphic-tees-fixture\",\"name\":\"Duplicate\"}")).andExpect(status().isConflict());
  mvc.perform(delete("/api/admin/collections/women").with(user("owner").roles("ADMIN"))).andExpect(status().isForbidden());
 }
 @Test void assignsProductsRemovesOnlyMembershipAndRejectsStaleSaves()throws Exception{
  create("custom-fixture","Custom Fixture");
  Product p=json.readValue(mvc.perform(get("/api/products/cherry-zest-oversized-t-shirt")).andReturn().getResponse().getContentAsString(),Product.class);
  p.id=null;p.version=null;p.slug="collection-membership-fixture";p.collections=new ArrayList<>(List.of("women","custom-fixture"));
  p=json.readValue(mvc.perform(post("/api/admin/products").with(user("owner").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(p))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),Product.class);
  mvc.perform(get("/api/collections")).andExpect(jsonPath("$[?(@.slug == 'custom-fixture')].productCount",contains(1)));
  mvc.perform(delete("/api/admin/collections/custom-fixture").with(user("owner").roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/collections")).andExpect(jsonPath("$[?(@.slug == 'custom-fixture')]",empty()));
  mvc.perform(get("/api/products/"+p.slug)).andExpect(status().isOk()).andExpect(jsonPath("$.collections",contains("women"))).andExpect(jsonPath("$.media",not(empty())));
  // Re-adding an old slug must not restore the removed assignments.
  create("custom-fixture","Custom Fixture");
  mvc.perform(get("/api/collections")).andExpect(jsonPath("$[?(@.slug == 'custom-fixture')].productCount",contains(0)));
  mvc.perform(put("/api/admin/products/"+p.id).with(user("owner").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(p))).andExpect(status().isConflict());
 }
 @Test void allowsProductsWithoutACollectionButRejectsUnknownMembership()throws Exception{
  Product p=json.readValue(mvc.perform(get("/api/products/cherry-zest-oversized-t-shirt")).andReturn().getResponse().getContentAsString(),Product.class);
  p.id=null;p.version=null;p.slug="no-collection-fixture";p.collections=new ArrayList<>();
  mvc.perform(post("/api/admin/products").with(user("owner").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(p))).andExpect(status().isCreated());
  p.slug="unknown-collection-fixture";p.collections=List.of("does-not-exist");
  mvc.perform(post("/api/admin/products").with(user("owner").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(p))).andExpect(status().isBadRequest());
 }
}
