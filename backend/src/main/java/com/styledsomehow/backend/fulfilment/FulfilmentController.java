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
 @GetMapping public FulfilmentService.Admin get(@PathVariable String id){return service.admin(id);}
 @PostMapping public FulfilmentService.Admin update(@PathVariable String id,@Valid @RequestBody Update request,Principal principal){return service.advance(id,request.expectedStage(),request.stage(),request.note(),principal.getName());}
}
