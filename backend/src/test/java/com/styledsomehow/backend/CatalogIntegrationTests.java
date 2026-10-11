package com.styledsomehow.backend;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.styledsomehow.backend.catalog.*;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.*;
@SpringBootTest @AutoConfigureMockMvc
class CatalogIntegrationTests {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Autowired ProductRepository repository;
 @org.springframework.test.context.DynamicPropertySource
 static void credentials(org.springframework.test.context.DynamicPropertyRegistry registry){registry.add("admin.password-hash",()->new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("integration-password-only"));}
 @Test void sessionLoginLogoutAndCsrf()throws Exception{
  mvc.perform(post("/api/auth/login").servletPath("/api/auth/login").param("username","test-owner").param("password","integration-password-only")).andExpect(status().isForbidden());
  mvc.perform(post("/api/auth/login").servletPath("/api/auth/login").with(csrf()).param("username","test-owner").param("password","incorrect")).andExpect(status().isUnauthorized());
  var login=mvc.perform(post("/api/auth/login").servletPath("/api/auth/login").with(csrf()).param("username","test-owner").param("password","integration-password-only")).andExpect(status().isNoContent()).andReturn();
  var session=(org.springframework.mock.web.MockHttpSession)login.getRequest().getSession(false);
  org.assertj.core.api.Assertions.assertThat(session).isNotNull();
  mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("test-owner"));
  mvc.perform(post("/api/auth/logout").servletPath("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/admin/products")).andExpect(status().isUnauthorized());
 }
 @Test void mediaRejectsScriptsAndAcceptsPhoto()throws Exception{
  var bad=new org.springframework.mock.web.MockMultipartFile("file","fake.png","image/png","<script>alert(1)</script>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
  mvc.perform(multipart("/api/admin/media").file(bad).with(user("owner").roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest());
  byte[] png=java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jFioAAAAASUVORK5CYII=");
  var good=new org.springframework.mock.web.MockMultipartFile("file","photo.png","image/png",png);
  String body=mvc.perform(multipart("/api/admin/media").file(good).with(user("owner").roles("ADMIN")).with(csrf())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String url=json.readTree(body).get("url").asText();
  mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().bytes(png));
  java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("build/test-media",url.substring(url.lastIndexOf('/')+1)));
 }
 @Test void publicSeedAndProtection()throws Exception{
  mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(6)));
  mvc.perform(get("/api/products/chilli-crush-oversized-t-shirt")).andExpect(jsonPath("$.price").value(699));
  mvc.perform(get("/api/admin/products")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/admin/products").with(user("owner").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/products").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
 }
 @Test void draftPublishEditArchiveAndConflict()throws Exception{
  // Fetch through HTTP to fully initialize ordered media/collections.
  String seed=mvc.perform(get("/api/products/cherry-zest-oversized-t-shirt")).andReturn().getResponse().getContentAsString();
  Product p=json.readValue(seed,Product.class);p.id=null;p.version=null;p.slug="admin-integration-fixture";p.name="Admin integration fixture";p.status=Product.Status.DRAFT;
  String created=mvc.perform(post("/api/admin/products").with(user("owner").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(p))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  p=json.readValue(created,Product.class);
  try{
   mvc.perform(get("/api/products/"+p.slug)).andExpect(status().isNotFound());
   p.status=Product.Status.PUBLISHED;p.price=649;
   String body=mvc.perform(put("/api/admin/products/"+p.id).with(user("owner").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(p))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   Product published=json.readValue(body,Product.class);
   mvc.perform(get("/api/products/"+p.slug)).andExpect(status().isOk()).andExpect(jsonPath("$.price").value(649));
   mvc.perform(put("/api/admin/products/"+p.id).with(user("owner").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(p))).andExpect(status().isConflict());
   published.status=Product.Status.ARCHIVED;
   mvc.perform(put("/api/admin/products/"+p.id).with(user("owner").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(published))).andExpect(status().isOk());
   mvc.perform(get("/api/products/"+p.slug)).andExpect(status().isNotFound());
  }finally{repository.deleteById(p.id);}
 }
 @Test void rejectsInvalidPricingAndUnsupportedSize()throws Exception{
  Product p=json.readValue(mvc.perform(get("/api/products/cherry-zest-oversized-t-shirt")).andReturn().getResponse().getContentAsString(),Product.class);
  p.id=null;p.version=null;p.slug="invalid-fixture";p.originalPrice=100;
  mvc.perform(post("/api/admin/products").with(user("owner").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(p))).andExpect(status().isBadRequest());
  p.originalPrice=999;p.sizes=java.util.List.of("XXL");
  mvc.perform(post("/api/admin/products").with(user("owner").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(p))).andExpect(status().isBadRequest());
 }
}
