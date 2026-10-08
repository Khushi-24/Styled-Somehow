package com.styledsomehow.backend.checkout;
import com.styledsomehow.backend.catalog.*;
import com.styledsomehow.backend.inventory.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
import static com.styledsomehow.backend.checkout.CheckoutDtos.*;
@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class CheckoutService {
 private final ProductRepository products;private final StockRepository stocks;private final OrderRepository orders;
 public CheckoutService(ProductRepository products,StockRepository stocks,OrderRepository orders){this.products=products;this.stocks=stocks;this.orders=orders;}
 private List<BlankStock> prepare(){var all=stocks.lockedAll();expire(all);return all;}
 private void expire(List<BlankStock> all){for(var order:orders.findByStatus("PENDING_PAYMENT"))if(!order.expiresAt.isAfter(Instant.now()))release(order,all,"EXPIRED");}
 private void release(PendingOrder order,List<BlankStock> all,String status){
  for(var line:order.items){var stock=all.stream().filter(s->s.id.equals(line.stockId)).findFirst().orElseThrow();if(stock.reserved<line.quantity)throw new IllegalStateException("Reservation balance is inconsistent");stock.reserved-=line.quantity;}
  order.status=status;orders.save(order);
 }
 public void expireReservations(){prepare();}
 public Quote quote(Cart cart){return calculate(cart.items(),prepare());}
 private Quote calculate(List<Item> items,List<BlankStock> all){
  Map<String,Item> combined=new LinkedHashMap<>();
  for(var item:items){String key=item.slug()+"|"+item.colour()+"|"+item.size();var old=combined.get(key);int count=item.quantity()+(old==null?0:old.quantity());if(count>20)throw bad("Maximum 20 of one product variant per checkout");combined.put(key,new Item(item.slug(),item.colour(),item.size(),count));}
  Map<Long,Integer> needed=new HashMap<>();List<Line> lines=new ArrayList<>();int subtotal=0;
  for(var item:combined.values()){
   var p=products.findBySlug(item.slug()).filter(v->v.status==Product.Status.PUBLISHED).orElseThrow(()->bad("A product in your cart is no longer available. Remove it and try again."));
   if(!p.colours.contains(item.colour())||!p.sizes.contains(item.size()))throw bad("A selected colour or size is no longer offered");
   var stock=all.stream().filter(s->s.colour.equals(item.colour())&&s.size.equals(item.size())).findFirst().orElseThrow(()->bad("Selected colour and size are unavailable"));
   int demand=needed.merge(stock.id,item.quantity(),Integer::sum);if(demand>stock.quantity-stock.reserved)throw new ResponseStatusException(HttpStatus.CONFLICT,"Not enough stock for "+item.colour()+" · "+item.size()+" across the items in your cart. Reduce quantities.");
   String image=p.media.stream().filter(m->m.colour.equals(item.colour())&&m.kind.equals("image")).findFirst().orElseThrow().url;
   lines.add(new Line(p.slug,p.name,item.colour(),item.size(),item.quantity(),p.price,image,stock.quantity-stock.reserved));subtotal=Math.addExact(subtotal,Math.multiplyExact(p.price,item.quantity()));
  }
  return new Quote(lines,subtotal,0,subtotal);
 }
 public PendingOrder create(Checkout request,String sessionKey){
  var all=prepare();String fingerprint=fingerprint(request);
  var previous=orders.findBySessionKeyAndRequestId(sessionKey,request.requestId());
  if(previous.isPresent()){var order=previous.get();if(!order.requestFingerprint.equals(fingerprint))throw new ResponseStatusException(HttpStatus.CONFLICT,"Checkout reference already used for different details");return order;}
  if(orders.existsBySessionKeyAndStatus(sessionKey,"PENDING_PAYMENT"))throw new ResponseStatusException(HttpStatus.CONFLICT,"You already have a pending checkout. Resume or cancel it before starting another.");
  var quote=calculate(request.items(),all);if(quote.total()!=request.expectedTotal())throw new ResponseStatusException(HttpStatus.CONFLICT,"Prices changed. Refresh your cart and review the updated total.");
  var order=new PendingOrder();order.id=UUID.randomUUID().toString();order.sessionKey=sessionKey;order.requestId=request.requestId();order.requestFingerprint=fingerprint;order.status="PENDING_PAYMENT";order.createdAt=Instant.now();order.expiresAt=order.createdAt.plusSeconds(900);order.subtotal=quote.subtotal();order.shipping=0;order.total=quote.total();
  var a=request.address();order.name=a.name().trim();order.email=a.email().trim();order.phone=a.phone();order.line1=a.line1().trim();order.line2=a.line2()==null?"":a.line2().trim();order.city=a.city().trim();order.state=a.state().trim();order.pinCode=a.pinCode();
  for(var line:quote.items()){
   var stock=all.stream().filter(s->s.colour.equals(line.colour())&&s.size.equals(line.size())).findFirst().orElseThrow();stock.reserved+=line.quantity();
   var snapshot=new PendingOrder.OrderLine();snapshot.stockId=stock.id;snapshot.slug=line.slug();snapshot.name=line.name();snapshot.colour=line.colour();snapshot.size=line.size();snapshot.quantity=line.quantity();snapshot.unitPrice=line.unitPrice();snapshot.image=line.image();order.items.add(snapshot);
  }
  stocks.flush();return orders.saveAndFlush(order);
 }
 public PendingOrder get(String id,String sessionKey){prepare();return owned(id,sessionKey);}
 public PendingOrder cancel(String id,String sessionKey){var all=prepare();var order=owned(id,sessionKey);if(order.status.equals("PENDING_PAYMENT"))release(order,all,"CANCELLED");return order;}
 private PendingOrder owned(String id,String sessionKey){return orders.findById(id).filter(o->o.sessionKey.equals(sessionKey)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 public List<PendingOrder> adminOrders(){prepare();return orders.findTop100ByOrderByCreatedAtDesc();}
 private String fingerprint(Checkout request){
  var values=new ArrayList<String>();values.add(Integer.toString(request.items().size()));for(var i:request.items()){values.add(i.slug());values.add(i.colour());values.add(i.size());values.add(Integer.toString(i.quantity()));}
  var a=request.address();values.addAll(Arrays.asList(a.name(),a.email(),a.phone(),a.line1(),a.line2(),a.city(),a.state(),a.pinCode(),Integer.toString(request.expectedTotal())));
  var text=new StringBuilder();for(String v:values){if(v==null)text.append("-1:");else text.append(v.length()).append(':').append(v);}return digest(text.toString());
 }
 private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 public static String digest(String value){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
