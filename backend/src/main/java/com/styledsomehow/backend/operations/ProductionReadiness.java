package com.styledsomehow.backend.operations;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import com.styledsomehow.backend.orderaccess.EmailDelivery;
import java.net.URI;
@Component @ConditionalOnProperty(name="app.production",havingValue="true")
public class ProductionReadiness {
 public ProductionReadiness(@Value("${server.servlet.session.cookie.secure:false}") boolean secure,@Value("${email.store-url:}") String store,@Value("${order-access.secret:}") String secret,@Value("${email.confirmations.enabled:false}") boolean enabled,EmailDelivery mail){
  URI origin=URI.create(store);if(!secure||!"https".equals(origin.getScheme())||origin.getHost()==null||origin.getUserInfo()!=null||origin.getQuery()!=null||origin.getFragment()!=null||!(origin.getPath().isEmpty()||origin.getPath().equals("/"))||secret.length()<32||!enabled||!mail.ready())throw new IllegalStateException("Production requires secure cookies, an HTTPS STORE_URL origin, ORDER_ACCESS_SECRET and configured order email. See deploy/README.md.");
 }
}
