package com.styledsomehow.backend.operations;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.security.Principal;
import java.time.Instant;
@RestController @RequestMapping("/api/admin/settings")
public class StoreSettingsController {
 private final CommerceControlRepository controls;
 public StoreSettingsController(CommerceControlRepository controls){this.controls=controls;}
 public record Settings(boolean ordersPaused){}
 @GetMapping public Settings get(){return new Settings(controls.findById(1L).orElseThrow().ordersPaused);}
 @PostMapping @Transactional public Settings update(@RequestBody Settings settings,Principal actor){var control=controls.lock();control.ordersPaused=settings.ordersPaused();control.updatedAt=Instant.now();control.updatedBy=actor.getName();return new Settings(control.ordersPaused);}
}
