package com.styledsomehow.backend;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.orderaccess.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.styledsomehow.backend.checkout.CheckoutDtos.*;
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@SpringBootTest(properties="order-access.secret=integration-test-only-not-a-deployed-secret-123456789") @Transactional
class OrderAccessIntegrationTests {
 @Autowired OrderAccessService access;@Autowired CheckoutService checkout;@Autowired ChallengeRepository challenges;
 @MockitoBean EmailDelivery mail;
 @Autowired org.springframework.test.web.servlet.MockMvc mvc;
 @Autowired tools.jackson.databind.ObjectMapper json;
 private String sent;
 @BeforeEach void setup(){when(mail.ready()).thenReturn(true);doAnswer(call->{sent=call.getArgument(1);return null;}).when(mail).sendCode(anyString(),anyString());}
 private PendingOrder order(){return checkout.create(new Checkout(List.of(new Item("cherry-zest-oversized-t-shirt","White","S",1)),new Address("Test","fixture@example.test","9999999999","Test","","Ahmedabad","Gujarat","380054"),UUID.randomUUID().toString(),599),UUID.randomUUID().toString());}
 @Test void codeIsBrowserBoundSingleUseAndHashed(){var o=order();var r=access.request(o.id,"FIXTURE@example.test","browser","127.0.0.1");assertThat(sent).matches("[0-9]{6}");assertThat(challenges.findById(r.challengeId()).orElseThrow().codeHash).hasSize(64).isNotEqualTo(sent);assertThat(access.verify(r.challengeId(),sent,"other-browser")).isEmpty();assertThat(access.verify(r.challengeId(),sent,"browser")).contains(o.id);assertThat(access.verify(r.challengeId(),sent,"browser")).isEmpty();}
 @Test void fiveWrongAttemptsLockCodeAndExpiredCodesFail(){var o=order();var r=access.request(o.id,o.email,"browser","127.0.0.1");String correct=sent;String wrong=correct.equals("000000")?"111111":"000000";for(int i=0;i<5;i++)assertThat(access.verify(r.challengeId(),wrong,"browser")).isEmpty();assertThat(challenges.findById(r.challengeId()).orElseThrow().attempts).isEqualTo(5);assertThat(access.verify(r.challengeId(),correct,"browser")).isEmpty();var c=challenges.findById(r.challengeId()).orElseThrow();c.attempts=0;c.expiresAt=Instant.now().minusSeconds(1);assertThat(access.verify(r.challengeId(),correct,"browser")).isEmpty();}
 @Test void mismatchedDetailsDoNotSendOrRevealOrder(){var o=order();var r=access.request(o.id,"wrong@example.test","browser","127.0.0.1");verify(mail,never()).sendCode(anyString(),anyString());assertThat(r.message()).startsWith("If the reference and email match");assertThat(access.verify(r.challengeId(),"123456","browser")).isEmpty();}
 @Test void httpAccessRequiresCsrfAndVerificationAndCanBeClosed()throws Exception{
  var o=order();var session=new org.springframework.mock.web.MockHttpSession();String originalSession=session.getId();String body=json.writeValueAsString(Map.of("orderId",o.id,"email",o.email));
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/order-access/order/"+o.id).session(session)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/order-access/request").session(session).contentType("application/json").content(body)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
  var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/order-access/request").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).session(session).contentType("application/json").content(body)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn().getResponse().getContentAsString();
  assertThat(response).doesNotContain(sent);String id=json.readTree(response).get("challengeId").asText();
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/order-access/verify").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).session(session).contentType("application/json").content(json.writeValueAsString(Map.of("challengeId",id,"code",sent)))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
  assertThat(session.getId()).isNotEqualTo(originalSession);
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/order-access/order/"+o.id).session(session)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value(o.id)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.sessionKey").doesNotExist());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/order-access/close").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).session(session)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/order-access/order/"+o.id).session(session)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
 }
 @Test void resendIsLimitedAndFailedDeliveryDoesNotUndoLimits(){var o=order();doThrow(new IllegalStateException("fixture provider failure")).when(mail).sendCode(anyString(),anyString());var r=access.request(o.id,o.email,"browser","127.0.0.1");assertThat(challenges.findById(r.challengeId()).orElseThrow().orderId).isNull();assertThatThrownBy(()->access.request(o.id,o.email,"new-browser","127.0.0.2")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);}
}
