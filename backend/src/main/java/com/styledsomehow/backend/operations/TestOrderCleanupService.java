package com.styledsomehow.backend.operations;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.inventory.*;
import com.styledsomehow.backend.payments.*;
import com.styledsomehow.backend.shipping.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class TestOrderCleanupService {
 private final OrderRepository orders;private final StockRepository stocks;private final MovementRepository movements;private final ShipmentRepository shipments;private final EntityManager em;
 public TestOrderCleanupService(OrderRepository orders,StockRepository stocks,MovementRepository movements,ShipmentRepository shipments,EntityManager em){this.orders=orders;this.stocks=stocks;this.movements=movements;this.shipments=shipments;this.em=em;}
 public record Stock(long stockId,String colour,String size,int onHand,int reserved,int deductedUnits,int reservationToRelease){}
 public record Preview(String orderId,String status,String fingerprint,int lineCount,long paymentCount,long eventCount,long challengeCount,boolean restorationAvailable,List<Stock> stock){}
 public record Result(String orderId,boolean alreadyDeleted,int restoredUnits,int releasedUnits){}
 private record Snapshot(Preview preview,Map<Long,Integer> quantities){}
 private PendingOrder order(String id){return orders.lockForShipping(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found. It may already have been removed."));}
 private static ResponseStatusException conflict(String s){return new ResponseStatusException(HttpStatus.CONFLICT,s);}
 public Preview preview(String id){var all=stocks.lockedAll();return snapshot(order(id),all).preview();}
 private Snapshot snapshot(PendingOrder o,List<BlankStock> all){
  if(!"TEST".equals(o.paymentMode))throw conflict("Only explicitly marked TEST orders can be deleted. LIVE and unassigned orders are protected.");
  if(!List.of("PENDING_PAYMENT","PAID","CANCELLED","EXPIRED","REFUNDED").contains(o.status==null?"":o.status))throw conflict("Resolve this payment or refund review before deleting the test order.");
  if(o.gatewayStartUntil!=null&&o.gatewayStartUntil.isAfter(Instant.now()))throw conflict("Payment preparation is in progress. Wait and refresh before deleting.");
  if(shipments.existsById(o.id))throw conflict("This order has a shipment record. Resolve it before deleting; cleanup never cancels a courier booking.");
  var receipts=em.createQuery("select r from PaymentReceipt r where r.orderId = :id order by r.id",PaymentReceipt.class).setParameter("id",o.id).getResultList();
  if(receipts.stream().anyMatch(r->!List.of("PAID","REFUNDED").contains(r.status==null?"":r.status)||r.workerLeaseUntil!=null&&r.workerLeaseUntil.isAfter(Instant.now())))throw conflict("Payment/refund work is active or needs review. Complete it before deleting.");
  var quantities=new TreeMap<Long,Integer>();for(var l:o.items){if(l.stockId==null||l.quantity<=0)throw conflict("Stock references need review before cleanup.");quantities.merge(l.stockId,l.quantity,Math::addExact);}
  var rows=new ArrayList<Stock>();var parts=new ArrayList<String>();parts.add(o.id);parts.add(o.status);parts.add(o.paymentMode);parts.add(Objects.toString(o.paymentId,""));parts.add(Objects.toString(o.gatewayOrderId,""));parts.add(Objects.toString(o.paidAt,""));parts.add(Objects.toString(o.gatewayStartUntil,""));parts.add(Objects.toString(o.fulfilmentStage,""));
  boolean canRestore=List.of("PAID","REFUNDED").contains(o.status==null?"":o.status)&&o.paymentId!=null&&o.paidAt!=null&&!quantities.isEmpty();
  for(var entry:quantities.entrySet()){
   var s=all.stream().filter(v->v.id.equals(entry.getKey())).findFirst().orElseThrow(()->conflict("A stock variant is missing. Review inventory before cleanup."));int units=entry.getValue();boolean pending="PENDING_PAYMENT".equals(o.status);
   if(pending&&s.reserved<units)throw conflict("Reservation balance needs review. No stock was changed.");
   var deduction=o.paymentId==null?Optional.<StockMovement>empty():movements.findByRequestId(UUID.nameUUIDFromBytes((o.paymentId+":"+s.id).getBytes(StandardCharsets.UTF_8)).toString());
   boolean verified=deduction.filter(m->s.id.equals(m.stockId)&&m.delta==-units&&("TEST paid order "+o.id).equals(m.reason)).isPresent();canRestore&=verified;
   rows.add(new Stock(s.id,s.colour,s.size,s.quantity,s.reserved,verified?units:0,pending?units:0));
   parts.add(s.id+":"+s.version+":"+s.quantity+":"+s.reserved+":"+units+":"+verified);
  }
  for(var r:receipts)parts.add(r.id+":"+r.status+":"+r.amount+":"+r.updatedAt+":"+r.workerLeaseUntil);
  long events=count("fulfilment_events",o.id),challenges=count("order_access_challenges",o.id);parts.add("events:"+events+":challenges:"+challenges);
  try{String fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(String.join("|",parts).getBytes(StandardCharsets.UTF_8)));return new Snapshot(new Preview(o.id,o.status,fingerprint,o.items.size(),receipts.size(),events,challenges,canRestore,List.copyOf(rows)),quantities);}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 private long count(String table,String id){return ((Number)em.createNativeQuery("select count(*) from "+table+" where order_id = :id").setParameter("id",id).getSingleResult()).longValue();}
 public Result delete(String id,String fingerprint,boolean restore,boolean stockConfirmed,String confirmation,String reason,String actor){
  if(!"DELETE TEST".equals(confirmation))throw conflict("Type DELETE TEST to confirm this test order deletion.");
  // Stock first, then order: same lock order as checkout/payment stock application.
  var all=stocks.lockedAll();var previous=em.find(TestOrderDeletion.class,id);
  if(previous!=null)return new Result(id,true,previous.restoredUnits,previous.releasedUnits);
  var o=order(id);var snapshot=snapshot(o,all);var preview=snapshot.preview();
  if(!preview.fingerprint().equals(fingerprint))throw conflict("Order or inventory changed after the preview. Refresh the cleanup preview before deleting.");
  if(restore&&(!stockConfirmed||!preview.restorationAvailable()))throw conflict("Stock restoration needs a verified test deduction and confirmation that it has not already been restored or physically used.");
  int restored=0,released=0;
  for(var row:preview.stock()){
   var s=all.stream().filter(v->v.id.equals(row.stockId())).findFirst().orElseThrow();
   if(row.reservationToRelease()>0){s.reserved-=row.reservationToRelease();released=Math.addExact(released,row.reservationToRelease());}
   if(restore){int units=row.deductedUnits();if((long)s.quantity+units>1000000)throw conflict("Restoration would exceed the stock limit.");s.quantity+=units;restored=Math.addExact(restored,units);
    var movement=new StockMovement();movement.requestId=UUID.nameUUIDFromBytes(("test-cleanup:"+id+":"+s.id).getBytes(StandardCharsets.UTF_8)).toString();movement.stockId=s.id;movement.colour=s.colour;movement.size=s.size;movement.delta=units;movement.resultingQuantity=s.quantity;movement.reason="Restored test order "+id+": "+reason.trim();movement.actor=actor.substring(0,Math.min(100,actor.length()));movement.occurredAt=Instant.now();movements.save(movement);
   }
  }
  var audit=new TestOrderDeletion();audit.orderId=id;audit.previousStatus=o.status;audit.actor=actor.substring(0,Math.min(100,actor.length()));audit.reason=reason.trim();audit.deletedAt=Instant.now();audit.stockRestored=restore;audit.restoredUnits=restored;audit.releasedUnits=released;em.persist(audit);em.flush();
  for(String table:List.of("order_access_challenges","fulfilment_events","shipment_bookings","payment_receipts","pending_order_lines"))em.createNativeQuery("delete from "+table+" where order_id = :id").setParameter("id",id).executeUpdate();
  orders.delete(o);orders.flush();return new Result(id,false,restored,released);
 }
}
