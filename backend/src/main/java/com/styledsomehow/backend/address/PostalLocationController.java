package com.styledsomehow.backend.address;
import org.springframework.web.bind.annotation.*;
@RestController
public class PostalLocationController {
 @org.springframework.beans.factory.annotation.Autowired private com.styledsomehow.backend.operations.CommerceGuard guard;
 private final PostalLocationService locations;
 public PostalLocationController(PostalLocationService locations){this.locations=locations;}
 @GetMapping("/api/checkout/location/{pin}") public PostalLocationService.Location location(@PathVariable String pin,jakarta.servlet.http.HttpServletRequest request){guard.check("shipping",request.getRemoteAddr(),com.styledsomehow.backend.checkout.CheckoutController.key(request.getSession()),null);return locations.lookup(pin);}
}
