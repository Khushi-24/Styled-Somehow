package com.styledsomehow.backend.shipping;

import com.styledsomehow.backend.checkout.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShipmentService {
 private final OrderRepository orders;private final ShipmentRepository shipments;private final ShippingGateway rates;private final ShipmentGateway gateway;
 private final double parcelLength,parcelBreadth,parcelHeight;
 private final TransactionTemplate tx;private final boolean enabled;private final String pickup;
 public ShipmentService(OrderRepository orders,ShipmentRepository shipments,ShippingGateway rates,ShipmentGateway gateway,PlatformTransactionManager manager,@Value("${shiprocket.booking-enabled:false}") boolean enabled,@Value("${shiprocket.pickup-location:Home}") String pickup,@Value("${shiprocket.parcel-length-cm:0}") double parcelLength,@Value("${shiprocket.parcel-breadth-cm:0}") double parcelBreadth,@Value("${shiprocket.parcel-height-cm:0}") double parcelHeight){this.parcelLength=parcelLength;this.parcelBreadth=parcelBreadth;this.parcelHeight=parcelHeight;this.orders=orders;this.shipments=shipments;this.rates=rates;this.gateway=gateway;this.tx=new TransactionTemplate(manager);this.enabled=enabled;this.pickup=pickup;}
 public record Preview(int courierId,String courier,int quotedCost,int customerShipping,int quantity,String weight,String dimensions,String pickupLocation){}
 public record Status(boolean bookingEnabled,ShipmentBooking booking){}
 private record Work(Map<String,Object> payload,ShippingGateway.Rate rate){}
 public Status status(String id){if(!orders.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND);return new Status(enabled,shipments.findById(id).orElse(null));}
 static void eligible(PendingOrder order){
  if(!"PAID".equals(order.status)||!"LIVE".equals(order.paymentMode)||order.paidAt==null||order.paymentId==null||!order.paymentId.startsWith("pay_"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Only paid LIVE orders can be shipped. Test, unpaid, refunded and review orders are blocked.");
 }
 private void enabled(){if(!enabled)throw new ResponseStatusException(HttpStatus.CONFLICT,"Shipment booking is disabled. Complete Shiprocket KYC and live-payment setup before enabling it.");}
 private PendingOrder order(String id){return orders.lockForShipping(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 public Preview preview(String id){
  enabled();return tx.execute(status->{var o=order(id);eligible(o);if(shipments.existsById(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"A booking already exists. Refresh its status; do not create another.");int quantity=o.items.stream().mapToInt(i->i.quantity).sum();var rate=rates.rate(o.pinCode,quantity,o.subtotal);return new Preview(rate.courierId(),rate.courier(),rate.rupees(),o.shipping,quantity,"Up to 0.5 kg",dimensionsReady()?parcelLength+" × "+parcelBreadth+" × "+parcelHeight+" cm":"Not configured",pickup);});
 }
 public record Comparison(List<ShippingGateway.Rate> options,int customerShipping,int quantity,String pickupLocation,String deliveryPin,ShippingGateway.Parcel parcel){}
 public Comparison compare(String id,ShippingGateway.Parcel parcel){
  validateParcel(parcel);
  return tx.execute(status->{var o=order(id);if(!"PAID".equals(o.status)||!("LIVE".equals(o.paymentMode)||"TEST".equals(o.paymentMode)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Only paid orders can compare couriers.");if(shipments.existsById(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"A booking already exists. Refresh shipment status.");int quantity=o.items.stream().mapToInt(i->i.quantity).sum();return new Comparison(rates.options(o.pinCode,quantity,o.subtotal,parcel),o.shipping,quantity,pickup,o.pinCode,parcel);});
 }
 private static void validateParcel(ShippingGateway.Parcel parcel){if(parcel==null||!parcel.valid())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter actual packed weight and dimensions before comparing or booking.");}
 private boolean dimensionsReady(){return parcelLength>0&&parcelBreadth>0&&parcelHeight>0&&Double.isFinite(parcelLength)&&Double.isFinite(parcelBreadth)&&Double.isFinite(parcelHeight);}
 public ShipmentBooking book(String id,int expectedCost,int expectedCourier){
  if(!shipments.existsById(id)&&!dimensionsReady())throw new ResponseStatusException(HttpStatus.CONFLICT,"Confirm packed parcel dimensions before booking. Previous dimensions have been removed; book manually in Shiprocket using actual measurements.");
  return book(id,expectedCost,expectedCourier,new ShippingGateway.Parcel(0.5,parcelLength,parcelBreadth,parcelHeight));
 }
 public ShipmentBooking book(String id,int expectedCost,int expectedCourier,ShippingGateway.Parcel parcel){
  enabled();validateParcel(parcel);
  Work work=tx.execute(status->{var o=order(id);eligible(o);if(shipments.existsById(id))return null;
   int quantity=o.items.stream().mapToInt(i->i.quantity).sum();var rate=rates.options(o.pinCode,quantity,o.subtotal,parcel).stream().filter(option->option.courierId()==expectedCourier).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Selected courier is no longer available. Compare couriers again."));
   if(rate.rupees()!=expectedCost||rate.courierId()!=expectedCourier)throw new ResponseStatusException(HttpStatus.CONFLICT,"Courier or price changed. Refresh the shipping quote before confirming.");
   var payload=payload(o,quantity,pickup);payload.put("weight",parcel.weightKg());payload.put("length",parcel.lengthCm());payload.put("breadth",parcel.breadthCm());payload.put("height",parcel.heightCm());var b=new ShipmentBooking();b.orderId=id;b.status="CREATING";b.courierId=rate.courierId();b.courier=rate.courier();b.quotedCost=rate.rupees();b.createdAt=Instant.now();b.updatedAt=b.createdAt;b.message="Creation started. Do not book again while processing.";shipments.saveAndFlush(b);return new Work(payload,rate);
  });
  if(work==null)return shipments.findById(id).orElseThrow();
  try{
   var created=gateway.create(work.payload());
   tx.executeWithoutResult(status->{order(id);var b=shipments.findById(id).orElseThrow();b.providerOrderId=created.orderId();b.providerShipmentId=created.shipmentId();b.status="ORDER_CREATED";b.updatedAt=Instant.now();shipments.saveAndFlush(b);});
   tx.executeWithoutResult(status->{var o=order(id);eligible(o);var b=shipments.findById(id).orElseThrow();b.status="ASSIGNING";b.updatedAt=Instant.now();shipments.saveAndFlush(b);});
   var assigned=gateway.assign(created.shipmentId(),work.rate().courierId());
   tx.executeWithoutResult(status->{order(id);var b=shipments.findById(id).orElseThrow();b.awb=assigned.awb();b.courier=assigned.courier();b.status="BOOKED";b.message="Tracking number assigned. Pickup has not been scheduled.";b.updatedAt=Instant.now();shipments.saveAndFlush(b);});
  }catch(RuntimeException error){
   tx.executeWithoutResult(status->{order(id);var b=shipments.findById(id).orElseThrow();b.status="REVIEW_REQUIRED";b.message="Check Shiprocket using this store order reference before taking further action. The request may have succeeded; automatic retries are blocked.";b.updatedAt=Instant.now();shipments.saveAndFlush(b);});
  }
  return shipments.findById(id).orElseThrow();
 }
 public static Map<String,Object> payload(PendingOrder o,int quantity,String pickup){
  if(quantity<1||quantity>2)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Parcel measurements are required for more than two T-shirts.");
  var body=new LinkedHashMap<String,Object>();body.put("order_id",o.id);body.put("order_date",DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Kolkata")).format(o.createdAt));body.put("pickup_location",pickup);
  body.put("billing_customer_name",o.name);body.put("billing_last_name","");body.put("billing_address",o.line1);body.put("billing_address_2",o.line2==null?"":o.line2);body.put("billing_city",o.city);body.put("billing_pincode",o.pinCode);body.put("billing_state",o.state);body.put("billing_country","India");body.put("billing_email",o.email);body.put("billing_phone",o.phone);body.put("shipping_is_billing",true);
  body.put("order_items",o.items.stream().map(i->Map.of("name",i.name+" · "+i.colour+" · "+i.size,"sku",i.stockId+"-"+i.slug+"-"+i.colour+"-"+i.size,"units",i.quantity,"selling_price",i.unitPrice)).toList());
  body.put("payment_method","Prepaid");body.put("sub_total",o.subtotal);body.put("shipping_charges",o.shipping);body.put("weight",0.5);return body;
 }
}
