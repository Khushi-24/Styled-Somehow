package com.styledsomehow.backend.orderaccess;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
/** Both verified login methods grant the same read-only customer scope, never an admin role. */
final class CustomerLoginSession {
 private CustomerLoginSession(){}
 static void grant(String email,HttpServletRequest request){var session=request.getSession();synchronized(session){request.changeSessionId();session.removeAttribute("verified-order-id");session.removeAttribute("verified-order-until");session.setAttribute("customer-email",email);session.setAttribute("customer-until",Instant.now().plusSeconds(1800));}}
}
