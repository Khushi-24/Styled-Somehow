package com.styledsomehow.backend.shipping;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/orders/{id}/shipment")
public class ShipmentController {
 @org.springframework.beans.factory.annotation.Autowired private TrackingService tracking;
 private final ShipmentService service;
 public ShipmentController(ShipmentService service){this.service=service;}
 public record Confirm(@Min(1) int expectedCost,@Min(1) int courierId,@NotNull @Valid ShippingGateway.Parcel parcel){}
 public record DiagnosticRequest(@Min(1) long shiprocketOrderId,@NotNull @Valid ShippingGateway.Parcel parcel){}
 public record Reconcile(@Min(1) long providerOrderId){}
 @PostMapping("/reconcile") public ShipmentBooking reconcile(@PathVariable String id,@Valid @RequestBody Reconcile request){return service.reconcile(id,request.providerOrderId());}
 @PostMapping("/diagnostic") public ShippingGateway.Diagnostic diagnose(@PathVariable String id,@Valid @RequestBody DiagnosticRequest request){return service.diagnose(id,request.parcel(),request.shiprocketOrderId());}
 @PostMapping("/tracking/refresh") public com.styledsomehow.backend.fulfilment.FulfilmentService.View tracking(@PathVariable String id){return tracking.refresh(id);}
 @GetMapping public ShipmentService.Status status(@PathVariable String id){return service.status(id);}
 @PostMapping("/preview") public ShipmentService.Preview preview(@PathVariable String id){return service.preview(id);}
 @PostMapping("/compare") public ShipmentService.Comparison compare(@PathVariable String id,@Valid @RequestBody ShippingGateway.Parcel parcel){return service.compare(id,parcel);}
 @PostMapping public ShipmentBooking book(@PathVariable String id,@Valid @RequestBody Confirm request){return service.book(id,request.expectedCost(),request.courierId(),request.parcel());}
}
