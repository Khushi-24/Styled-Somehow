package com.styledsomehow.backend.orderaccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
@RestController
public class CustomerLoginProviders {
 private final boolean google;
 public CustomerLoginProviders(@Value("${auth.google.enabled:false}") boolean google){this.google=google;}
 public record Providers(boolean google){}
 @GetMapping("/api/order-access/login/providers") public ResponseEntity<Providers> providers(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Providers(google));}
}
