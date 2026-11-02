package com.styledsomehow.backend.orderaccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
/** Dedicated chain keeps Google customer identities separate from admin Spring Security sessions. */
@Configuration @ConditionalOnProperty(name="auth.google.enabled",havingValue="true")
public class GoogleCustomerLoginConfiguration {
 @Bean ClientRegistrationRepository customerGoogleRegistration(@Value("${auth.google.client-id:}") String id,@Value("${auth.google.client-secret:}") String secret,@Value("${email.store-url:http://localhost:3000}") String store){
  if(id.isBlank()||secret.isBlank())throw new IllegalStateException("Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET before enabling Google login");
  String origin=GoogleCustomerLoginHandler.storeOrigin(store);
  return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId(id).clientSecret(secret).scope("openid","email").redirectUri(origin+"/api/order-access/google/callback/google").build());
 }
 @Bean @Order(1) SecurityFilterChain googleCustomerSecurity(HttpSecurity http,ClientRegistrationRepository registrations,@Value("${email.store-url:http://localhost:3000}") String store)throws Exception{
  String origin=GoogleCustomerLoginHandler.storeOrigin(store);var clients=new HttpSessionOAuth2AuthorizedClientRepository();var contexts=new NullSecurityContextRepository();
  http.securityMatcher("/api/order-access/google/**")
   .authorizeHttpRequests(a->a.anyRequest().permitAll())
   .securityContext(c->c.securityContextRepository(contexts))
   .requestCache(c->c.disable())
   .oauth2Login(o->o.clientRegistrationRepository(registrations).authorizedClientRepository(clients).securityContextRepository(contexts)
    .loginPage(origin+"/orders")
    .authorizationEndpoint(a->a.baseUri("/api/order-access/google/authorize"))
    .redirectionEndpoint(e->e.baseUri("/api/order-access/google/callback/*"))
    .successHandler(new GoogleCustomerLoginHandler(origin,clients))
    .failureHandler((request,response,error)->response.sendRedirect(origin+"/orders?login=google-failed")));
  return http.build();
 }
}
