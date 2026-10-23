package com.styledsomehow.backend.costing;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.util.Map;
import static com.styledsomehow.backend.costing.CostingDtos.*;
@RestController @RequestMapping("/api/admin/costing")
public class CostingController {
 private final CostingService service;
 public CostingController(CostingService service){this.service=service;}
 @GetMapping public Dashboard get(@RequestParam String month){return service.dashboard(month);}
 @PutMapping("/products/{id}") public Row save(@PathVariable long id,@RequestParam String month,@Valid @RequestBody Profile input){return service.save(id,month,input);}
 @PutMapping("/months/{month}") public Month month(@PathVariable String month,@Valid @RequestBody Month input){return service.saveMonth(month,input);}
 @ExceptionHandler({ObjectOptimisticLockingFailureException.class,DataIntegrityViolationException.class}) public ResponseEntity<Map<String,String>> conflict(){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","Costing changed in another tab. Reload before saving; keep a copy of your edits."));}
}
