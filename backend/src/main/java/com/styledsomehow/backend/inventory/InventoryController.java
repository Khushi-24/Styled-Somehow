package com.styledsomehow.backend.inventory;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
@RestController @RequestMapping("/api/admin/inventory")
public class InventoryController {
 private final InventoryService inventory;
 public InventoryController(InventoryService inventory){this.inventory=inventory;}
 @GetMapping public List<BlankStock> list(){return inventory.list();}
 @GetMapping("/movements") public List<StockMovement> history(){return inventory.history();}
 @PostMapping("/{id}/adjustments") public BlankStock adjust(@PathVariable Long id,@Valid @RequestBody InventoryService.Adjustment adjustment,Principal principal){return inventory.adjust(id,adjustment,principal.getName());}
}
