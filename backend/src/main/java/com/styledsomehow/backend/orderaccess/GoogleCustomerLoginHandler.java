package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.GmailAddress;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
/** Receives only Spring-validated Google OIDC tokens; never accepts a browser-supplied email as identity. */
final class GoogleCustomerLoginHandler implements AuthenticationSuccessHandler {
 private final String origin;private final OAuth2AuthorizedClientRepository clients;
 GoogleCustomerLoginHandler(String origin,OAuth2AuthorizedClientRepository clients){this.origin=storeOrigin(origin);this.clients=clients;}
 static String storeOrigin(String raw){
  var uri=URI.create(raw);if(!("http".equals(uri.getScheme())||"https".equals(uri.getScheme()))||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!(uri.getPath().isEmpty()||uri.getPath().equals("/")))throw new IllegalArgumentException("STORE_URL must be a storefront origin without path, query or credentials");
  if(!"https".equals(uri.getScheme())&&!java.util.List.of("localhost","127.0.0.1","[::1]","::1").contains(uri.getHost()))throw new IllegalArgumentException("Public STORE_URL must use HTTPS");
  return raw.replaceAll("/+$","");
 }
 @Override public void onAuthenticationSuccess(HttpServletRequest request,HttpServletResponse response,Authentication authentication)throws IOException{
  // No refresh/access tokens are needed once the verified customer email is established.
  clients.removeAuthorizedClient("google",authentication,request,response);
  if(!(authentication instanceof OAuth2AuthenticationToken token)||!"google".equals(token.getAuthorizedClientRegistrationId())||!(authentication.getPrincipal() instanceof OidcUser user)||!Boolean.TRUE.equals(user.getIdToken().getClaimAsBoolean("email_verified"))){response.sendRedirect(origin+"/orders?login=google-failed");return;}
  String email=user.getIdToken().getClaimAsString("email");
  if(email==null||!GmailAddress.valid(email.trim())){response.sendRedirect(origin+"/orders?login=google-email");return;}
  CustomerLoginSession.grant(email.trim().toLowerCase(Locale.ROOT),request);response.sendRedirect(origin+"/orders");
 }
}
