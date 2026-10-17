package com.styledsomehow.backend.address;
import org.springframework.web.bind.annotation.*;
@RestController
public class PostalLocationController {
 private final PostalLocationService locations;
 public PostalLocationController(PostalLocationService locations){this.locations=locations;}
 @GetMapping("/api/checkout/location/{pin}") public PostalLocationService.Location location(@PathVariable String pin){return locations.lookup(pin);}
}
