package com.styledsomehow.backend.fulfilment;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.shipping.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
@Service
@Transactional
public class FulfilmentService {
 private final OrderRepository orders;private final ShipmentRepository shipments;private final FulfilmentEventRepository events;
 public FulfilmentService(OrderRepository orders,ShipmentRepository shipments,FulfilmentEventRepository events){this.orders=orders;this.shipments=shipments;this.events=events;}
 public record Event(String stage,Instant occurredAt){}
 public record View(String orderId,String stage,boolean test,boolean onHold,String courier,String awb,String courierStatus,String trackingUrl,Instant trackingCheckedAt,boolean trackingFailed,List<Event> timeline){}
 public record Admin(View fulfilment,List<FulfilmentEvent> history,List<String> allowedNext){}
 public static String stage(PendingOrder o){return o.fulfilmentStage==null||o.fulfilmentStage.equals("UNFULFILLED")?"PAID".equals(o.status)?"PAID":"UNFULFILLED":o.fulfilmentStage;}
 private PendingOrder lock(String id){return orders.lockForShipping(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 private boolean editable(PendingOrder o){return "PAID".equals(o.status)&&o.paidAt!=null&&o.paymentId!=null&&List.of("LIVE","TEST").contains(o.paymentMode==null?"":o.paymentMode);}
 public View customer(String id){
  var o=orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));var b=shipments.findById(id).orElse(null);
  boolean real="LIVE".equals(o.paymentMode);boolean tracked=real&&b!=null&&b.awb!=null&&"BOOKED".equals(b.status);
  return new View(id,stage(o),"TEST".equals(o.paymentMode),!"PAID".equals(o.status),tracked?b.courier:null,tracked?b.awb:null,tracked?b.courierStatus:null,tracked?b.trackingUrl:null,tracked?b.trackingCheckedAt:null,tracked&&b.trackingFailed,events.findByOrderIdOrderByOccurredAtAsc(id).stream().map(e->new Event(e.stage,e.occurredAt)).toList());
 }
 public Admin admin(String id){
  var o=orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  return new Admin(customer(id),events.findByOrderIdOrderByOccurredAtAsc(id),allowed(o));
 }
 private List<String> allowed(PendingOrder o){
  if(!editable(o))return List.of();
  return switch(stage(o)){
   case "PAID" -> List.of("PACKING");
   case "PACKING","PRINTING","PACKED" -> List.of("READY_TO_SHIP");
   case "COURIER_BOOKED" -> "LIVE".equals(o.paymentMode)&&shipments.findById(o.id).filter(b->"BOOKED".equals(b.status)&&b.awb!=null).isPresent()?List.of("SHIPPED"):List.of();
   default -> List.of();
  };
 }
 public Admin advance(String id,String expected,String next,String note,String actor){
  var o=lock(id);if(!editable(o))throw conflict("Only verified paid orders can enter fulfilment.");
  if(!stage(o).equals(expected))throw conflict("Fulfilment changed. Refresh this order before updating.");
  if(!allowed(o).contains(next))throw conflict("This fulfilment transition is not available.");
  if("SHIPPED".equals(next)&&(note==null||note.isBlank()))throw conflict("Confirm courier handover in the note before marking shipped.");
  change(o,next,actor,note==null?"":note.trim());orders.flush();return admin(id);
 }
 public void requireReady(PendingOrder o){if(!"READY_TO_SHIP".equals(stage(o)))throw conflict("Mark this order Ready to ship before creating its shipment.");}
 public void courierBooked(PendingOrder o){if("READY_TO_SHIP".equals(stage(o)))change(o,"COURIER_BOOKED","Shiprocket","AWB assigned. Pickup is not yet confirmed.");}
 public void courierProgress(PendingOrder o,String next){
  if(!editable(o)||!"LIVE".equals(o.paymentMode))return;
  String current=stage(o);
  if(List.of("DELIVERED","RETURNED_TO_ORIGIN").contains(current))return;
  if(!List.of("COURIER_BOOKED","SHIPPED","IN_TRANSIT","DELIVERY_ISSUE").contains(current))return;
  if("IN_TRANSIT".equals(current)&&"SHIPPED".equals(next))return;
  if(!current.equals(next)&&List.of("SHIPPED","IN_TRANSIT","DELIVERED","DELIVERY_ISSUE","RETURNED_TO_ORIGIN").contains(next))change(o,next,"Shiprocket","Updated from courier tracking.");
 }
 private void change(PendingOrder o,String next,String actor,String note){
  o.fulfilmentStage=next;var e=new FulfilmentEvent();e.id=UUID.randomUUID().toString();e.orderId=o.id;e.stage=next;e.actor=actor.length()>100?actor.substring(0,100):actor;e.note=note.length()>500?note.substring(0,500):note;e.occurredAt=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);var latest=events.latestTime(o.id);if(latest!=null&&!e.occurredAt.isAfter(latest))e.occurredAt=latest.plus(1,java.time.temporal.ChronoUnit.MICROS);e.notificationEligible=List.of("PACKING","READY_TO_SHIP","SHIPPED","IN_TRANSIT","DELIVERED").contains(next)&&!events.existsByOrderIdAndStageAndNotificationEligibleTrue(o.id,next);events.save(e);orders.save(o);
 }
 private static ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
