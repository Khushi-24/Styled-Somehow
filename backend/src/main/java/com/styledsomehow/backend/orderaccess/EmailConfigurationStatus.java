package com.styledsomehow.backend.orderaccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
/** Report readiness without logging passwords, addresses, tokens or provider responses. */
@Component
public class EmailConfigurationStatus {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(EmailConfigurationStatus.class);
 private final EmailDelivery mail;private final boolean enabled,linkReady;
 public EmailConfigurationStatus(EmailDelivery mail,@Value("${email.confirmations.enabled:false}") boolean enabled,@Value("${order-access.secret:}") String secret){this.mail=mail;this.enabled=enabled;this.linkReady=secret.length()>=32;}
 @EventListener(ApplicationReadyEvent.class) public void report(){
  if(!enabled||!mail.ready()||!linkReady)log.warn("Order confirmation email readiness: enabled={}, providerConfigured={}, orderLinkConfigured={}. Check ORDER_CONFIRMATION_EMAIL_ENABLED, EMAIL_PROVIDER, provider credentials and ORDER_ACCESS_SECRET in the backend environment.",enabled,mail.ready(),linkReady);
  else log.info("Order confirmation email configuration is present; provider connectivity is verified when sending a new paid-order confirmation.");
 }
}
