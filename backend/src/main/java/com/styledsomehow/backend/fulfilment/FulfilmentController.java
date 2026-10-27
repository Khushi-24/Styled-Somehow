package com.styledsomehow.backend.fulfilment;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
@RestController @RequestMapping("/api/admin/orders/{id}/fulfilment")
public class FulfilmentController {
 private final FulfilmentService service;
 public FulfilmentController(FulfilmentService service){this.service=service;}
 public record Update(@NotBlank @Size(max=30) String expectedStage,@NotBlank @Size(max=30) String stage,@NotNull @Size(max=500) String note){}
 public record MethodUpdate(@NotBlank @Size(max=30) String expectedStage,@NotBlank @Pattern(regexp="COURIER|PERSONAL") String expectedMethod,@NotBlank @Pattern(regexp="COURIER|PERSONAL") String method,@NotNull @Size(max=400) String note){}
 @PostMapping("/delivery-method") public FulfilmentService.Admin deliveryMethod(@PathVariable String id,@Valid @RequestBody MethodUpdate request,Principal principal){return service.deliveryMethod(id,request.expectedStage(),request.expectedMethod(),request.method(),request.note(),principal.getName());}
 @GetMapping public FulfilmentService.Admin get(@PathVariable String id){return service.admin(id);}
 @PostMapping public FulfilmentService.Admin update(@PathVariable String id,@Valid @RequestBody Update request,Principal principal){return service.advance(id,request.expectedStage(),request.stage(),request.note(),principal.getName());}
}
