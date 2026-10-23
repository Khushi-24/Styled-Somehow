package com.styledsomehow.backend;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"auth.google.enabled=true","auth.google.client-id=fixture.apps.googleusercontent.com","auth.google.client-secret=fixture-not-real","email.store-url=http://localhost:3000"}) @AutoConfigureMockMvc
class GoogleCustomerLoginIntegrationTests {
 @Autowired MockMvc mvc;
 @Test void googleStartUsesStateAndCorrectCallbackWithoutExposingClientSecret()throws Exception{
  var session=new MockHttpSession();String location=mvc.perform(get("/api/order-access/google/authorize/google").session(session)).andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
  String decoded=java.net.URLDecoder.decode(location,java.nio.charset.StandardCharsets.UTF_8);assertThat(decoded).startsWith("https://accounts.google.com/").contains("state=","nonce=","openid","email").contains("redirect_uri=http://localhost:3000/api/order-access/google/callback/google").doesNotContain("fixture-not-real");
 }
 @Test void unsolicitedCallbackCannotLogInAndCustomerCannotAccessAdmin()throws Exception{
  var session=new MockHttpSession();mvc.perform(get("/api/order-access/google/callback/google").param("code","fabricated").param("state","fabricated").session(session)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("http://localhost:3000/orders?login=google-failed"));assertThat(session.getAttribute("customer-email")).isNull();
  session.setAttribute("customer-email","fixture@gmail.com");session.setAttribute("customer-until",java.time.Instant.now().plusSeconds(500));mvc.perform(get("/api/admin/orders").session(session)).andExpect(status().isUnauthorized());
 }
 @Test void providersArePublicButExposeNoCredentials()throws Exception{
  String body=mvc.perform(get("/api/order-access/login/providers")).andExpect(status().isOk()).andExpect(jsonPath("$.google").value(true)).andReturn().getResponse().getContentAsString();assertThat(body).doesNotContain("fixture.apps","fixture-not-real");
 }
}
