package com.styledsomehow.backend;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.orderaccess.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest(properties="order-access.secret=integration-test-only-not-a-deployed-secret-123456789") @AutoConfigureMockMvc @Transactional
class EmailLoginIntegrationTests {
 @Autowired EmailLoginService login;@Autowired ChallengeRepository challenges;@Autowired OrderRepository orders;@Autowired MockMvc mvc;
 @MockitoBean EmailDelivery mail;
 private String code;
 @BeforeEach void setup(){when(mail.ready()).thenReturn(true);doAnswer(c->{code=c.getArgument(1);return null;}).when(mail).sendCode(anyString(),anyString());}
 private PendingOrder paid(String email,Instant created){var o=new PendingOrder();o.id=UUID.randomUUID().toString();o.email=email;o.createdAt=created;o.status="PAID";o.paymentId="pay_fixture";o.paidAt=created;o.expiresAt=created;o.paymentMode="TEST";o.name="Fixture";orders.saveAndFlush(o);return o;}
 @Test void loginWorksWithoutOrdersAndCodeIsBoundHashedSingleUse(){
  var result=login.request("NOORDERS@gmail.com","browser","ip");var c=challenges.findById(result.challengeId()).orElseThrow();String correct=code;
  assertThat(c.codeHash).hasSize(64).isNotEqualTo(correct);assertThat(c.loginEmail).isEqualTo("noorders@gmail.com");
  assertThat(login.verify(c.id,correct,"other-browser")).isEmpty();assertThat(login.verify(c.id,correct,"browser")).contains("noorders@gmail.com");assertThat(login.verify(c.id,correct,"browser")).isEmpty();
 }
 @Test void fiveWrongGuessesAndExpiryBlockVerification(){
  var result=login.request("fixture@gmail.com","browser","ip");String correct=code,wrong=correct.equals("000000")?"111111":"000000";
  for(int i=0;i<5;i++)assertThat(login.verify(result.challengeId(),wrong,"browser")).isEmpty();
  assertThat(login.verify(result.challengeId(),correct,"browser")).isEmpty();var c=challenges.findById(result.challengeId()).orElseThrow();c.attempts=0;c.expiresAt=Instant.now().minusSeconds(1);assertThat(login.verify(c.id,correct,"browser")).isEmpty();
 }
 @Test void aliasesShareHourlyLimitAndCooldown(){
  var result=login.request("fi.xture+one@gmail.com","one","ip");assertThatThrownBy(()->login.request("fixture+two@gmail.com","two","other-ip")).hasMessageContaining("60 seconds");
  var first=challenges.findById(result.challengeId()).orElseThrow();first.createdAt=Instant.now().minusSeconds(120);
  for(int i=0;i<2;i++){var c=new AccessChallenge();c.id=UUID.randomUUID().toString();c.emailHash=first.emailHash;c.createdAt=Instant.now().minusSeconds(120);challenges.saveAndFlush(c);}
  assertThatThrownBy(()->login.request("fixture@gmail.com","new","new-ip")).hasMessageContaining("3 codes per hour");
 }
 @Test void ipLimitCannotBeBypassedByChangingEmail(){
  var result=login.request("fixture@gmail.com","browser","shared-ip");var first=challenges.findById(result.challengeId()).orElseThrow();
  for(int i=0;i<19;i++){var c=new AccessChallenge();c.id=UUID.randomUUID().toString();c.ipHash=first.ipHash;c.createdAt=Instant.now().minusSeconds(120);challenges.saveAndFlush(c);}
  assertThatThrownBy(()->login.request("another@gmail.com","other","shared-ip")).hasMessageContaining("Too many code requests");
 }
 @Test void deliveryFailureCannotGrantLoginAndStillConsumesQuota(){
  doThrow(new IllegalStateException("fixture failure")).when(mail).sendCode(anyString(),anyString());
  assertThatThrownBy(()->login.request("fixture@gmail.com","browser","ip")).hasMessageContaining("couldn’t send");
  assertThat(challenges.findAll()).anySatisfy(c->{assertThat(c.loginEmail).isNull();assertThat(c.codeHash).hasSize(64);});
  assertThatThrownBy(()->login.request("fixture@gmail.com","other","other")).hasMessageContaining("60 seconds");
 }
 @Test void historyNeedsVerifiedSessionAndIsNewestFirstAndScoped()throws Exception{
  var old=paid("fixture@gmail.com",Instant.now().minusSeconds(500));var recent=paid("FIXTURE@gmail.com",Instant.now().minusSeconds(10));var other=paid("other@gmail.com",Instant.now());
  mvc.perform(get("/api/order-access/login/orders")).andExpect(status().isUnauthorized());
  var session=new MockHttpSession();String original=session.getId();String body=mvc.perform(post("/api/order-access/login/code").session(session).with(csrf()).contentType("application/json").content("{\"email\":\"fixture@gmail.com\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String challenge=new tools.jackson.databind.ObjectMapper().readTree(body).path("challengeId").asText();
  mvc.perform(post("/api/order-access/login/verify").session(session).with(csrf()).contentType("application/json").content("{\"challengeId\":\""+challenge+"\",\"code\":\""+code+"\"}")).andExpect(status().isOk());assertThat(session.getId()).isNotEqualTo(original);
  mvc.perform(get("/api/order-access/login/orders").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.orders[0].id").value(recent.id)).andExpect(jsonPath("$.orders[1].id").value(old.id)).andExpect(jsonPath("$.orders.length()").value(2));
  mvc.perform(get("/api/order-access/order/"+recent.id).session(session)).andExpect(status().isOk());mvc.perform(get("/api/order-access/order/"+other.id).session(session)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/order-access/order/"+other.id+"/fulfilment").session(session)).andExpect(status().isUnauthorized());
  session.setAttribute("customer-until",Instant.now().minusSeconds(1));mvc.perform(get("/api/order-access/login/orders").session(session)).andExpect(status().isUnauthorized());
 }
 @Test void loginRequiresCsrfAndLogoutClearsHistoryAccess()throws Exception{
  mvc.perform(post("/api/order-access/login/code").contentType("application/json").content("{\"email\":\"fixture@gmail.com\"}")).andExpect(status().isForbidden());
  var session=new MockHttpSession();session.setAttribute("customer-email","fixture@gmail.com");session.setAttribute("customer-until",Instant.now().plusSeconds(500));
  mvc.perform(post("/api/order-access/login/logout").session(session).with(csrf())).andExpect(status().isNoContent());mvc.perform(get("/api/order-access/login/orders").session(session)).andExpect(status().isUnauthorized());
 }
}
