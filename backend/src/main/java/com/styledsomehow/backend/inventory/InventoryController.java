package com.styledsomehow.backend.inventory;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
@RestController @RequestMapping("/api/admin/inventory")
public class InventoryController {
 private final InventoryService inventory;
 public InventoryController(InventoryService inventory){this.inventory=inventory;}
 @PostMapping public BlankStock create(@Valid @RequestBody InventoryService.NewVariant input){return inventory.create(input);}
 @GetMapping public List<BlankStock> list(){return inventory.list();}
 @GetMapping("/movements/search") public org.springframework.data.domain.Slice<StockMovement> movements(@RequestParam(defaultValue="0") int page){if(page<0||page>10000)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);return inventory.movements(page);}
 @GetMapping("/movements") public List<StockMovement> history(){return inventory.history();}
 @PostMapping("/{id}/adjustments") public BlankStock adjust(@PathVariable Long id,@Valid @RequestBody InventoryService.Adjustment adjustment,Principal principal){return inventory.adjust(id,adjustment,principal.getName());}
}
