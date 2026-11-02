package com.styledsomehow.backend.orderaccess;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class GoogleCustomerLoginTests {
 private OAuth2AuthenticationToken identity(String email,boolean verified){
  var claims=new HashMap<String,Object>();claims.put("sub","fixture-subject");claims.put("email",email);claims.put("email_verified",verified);
  var token=new OidcIdToken("fixture-not-a-real-token",Instant.now(),Instant.now().plusSeconds(600),claims);
  var user=new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")),token);
  return new OAuth2AuthenticationToken(user,user.getAuthorities(),"google");
 }
 @Test void verifiedGoogleIdentityGrantsOnlyEmailScopeRotatesSessionAndDropsTokens()throws Exception{
  var clients=mock(OAuth2AuthorizedClientRepository.class);var request=new MockHttpServletRequest();var response=new MockHttpServletResponse();var session=request.getSession();String old=session.getId();session.setAttribute("guest-checkout-key","retained");session.setAttribute("verified-order-id","old-order");session.setAttribute("SPRING_SECURITY_CONTEXT","existing-admin-context");
  var authentication=identity("FIXTURE@gmail.com",true);new GoogleCustomerLoginHandler("http://localhost:3000",clients).onAuthenticationSuccess(request,response,authentication);
  assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/orders");assertThat(session.getId()).isNotEqualTo(old);assertThat(session.getAttribute("customer-email")).isEqualTo("fixture@gmail.com");assertThat(session.getAttribute("customer-until")).isInstanceOf(Instant.class);assertThat(session.getAttribute("verified-order-id")).isNull();assertThat(session.getAttribute("guest-checkout-key")).isEqualTo("retained");assertThat(session.getAttribute("SPRING_SECURITY_CONTEXT")).isEqualTo("existing-admin-context");
  verify(clients).removeAuthorizedClient("google",authentication,request,response);
 }
 @Test void unverifiedAndNonGmailIdentitiesCannotGrantAccess()throws Exception{
  var handler=new GoogleCustomerLoginHandler("https://styledsomehow.in",mock(OAuth2AuthorizedClientRepository.class));
  for(var authentication:List.of(identity("fixture@gmail.com",false),identity("fixture@other.test",true))){var request=new MockHttpServletRequest();request.getSession();var response=new MockHttpServletResponse();handler.onAuthenticationSuccess(request,response,authentication);assertThat(request.getSession().getAttribute("customer-email")).isNull();assertThat(response.getRedirectedUrl()).startsWith("https://styledsomehow.in/orders?login=google-");}
 }
 @Test void redirectOriginRejectsUnsafeSchemesAndNonOriginUrls(){
  for(String url:List.of("javascript:alert(1)","https://user@styledsomehow.in","https://styledsomehow.in/elsewhere","https://styledsomehow.in?next=evil","http://example.test"))assertThatThrownBy(()->GoogleCustomerLoginHandler.storeOrigin(url)).isInstanceOf(IllegalArgumentException.class);
  assertThat(GoogleCustomerLoginHandler.storeOrigin("https://styledsomehow.in/")).isEqualTo("https://styledsomehow.in");
 }
}
