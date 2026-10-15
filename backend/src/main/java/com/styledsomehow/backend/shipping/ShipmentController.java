package com.styledsomehow.backend.shipping;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/orders/{id}/shipment")
public class ShipmentController {
 private final ShipmentService service;
 public ShipmentController(ShipmentService service){this.service=service;}
 public record Confirm(@Min(1) int expectedCost,@Min(1) int courierId){}
 @GetMapping public ShipmentService.Status status(@PathVariable String id){return service.status(id);}
 @PostMapping("/preview") public ShipmentService.Preview preview(@PathVariable String id){return service.preview(id);}
 @PostMapping public ShipmentBooking book(@PathVariable String id,@Valid @RequestBody Confirm request){return service.book(id,request.expectedCost(),request.courierId());}
}
