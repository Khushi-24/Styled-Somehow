package com.styledsomehow.backend.operations;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
@RestController @RequestMapping("/api/admin/orders/{id}/test-cleanup")
public class TestOrderCleanupController {
 private final TestOrderCleanupService service;
 public TestOrderCleanupController(TestOrderCleanupService service){this.service=service;}
 public record Delete(@NotBlank @Pattern(regexp="[a-f0-9]{64}") String fingerprint,@NotNull Boolean restoreStock,boolean stockConfirmed,@NotBlank @Size(max=20) String confirmation,@NotBlank @Size(max=300) String reason){}
 @GetMapping public TestOrderCleanupService.Preview preview(@PathVariable String id){return service.preview(id);}
 @DeleteMapping public TestOrderCleanupService.Result delete(@PathVariable String id,@Valid @RequestBody Delete input,Principal actor){return service.delete(id,input.fingerprint(),input.restoreStock(),input.stockConfirmed(),input.confirmation(),input.reason(),actor.getName());}
}
