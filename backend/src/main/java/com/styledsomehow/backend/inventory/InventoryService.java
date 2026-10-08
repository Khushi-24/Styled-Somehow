package com.styledsomehow.backend.inventory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
@Service
public class InventoryService {
 private final StockRepository stocks;private final MovementRepository movements;
 public InventoryService(StockRepository s,MovementRepository m){stocks=s;movements=m;}
 public record Adjustment(@NotNull Long version,@Min(-100000) @Max(100000) int delta,@NotBlank @Size(max=500) String reason,@NotBlank @Pattern(regexp="[a-f0-9-]{36}") String requestId){}
 @Transactional(readOnly=true) public List<BlankStock> list(){return stocks.findAll().stream().sorted(Comparator.comparing((BlankStock s)->s.colour).thenComparingInt(s->List.of("S","M","L","XL").indexOf(s.size))).toList();}
 @Transactional(readOnly=true) public Map<String,Map<String,Boolean>> availability(){Map<String,Map<String,Boolean>> result=new HashMap<>();for(var s:list())result.computeIfAbsent(s.colour,k->new HashMap<>()).put(s.size,s.quantity-s.reserved>0);return result;}
 @Transactional(readOnly=true) public List<StockMovement> history(){return movements.findTop100ByOrderByIdDesc();}
 @Transactional public BlankStock adjust(Long id,Adjustment a,String actor){
  var stock=stocks.locked(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  var previous=movements.findByRequestId(a.requestId());
  if(previous.isPresent()){
   var m=previous.get();if(!Objects.equals(m.stockId,id)||m.delta!=a.delta()||!m.reason.equals(a.reason().trim()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Adjustment reference already used");return stock;
  }
  if(!Objects.equals(stock.version,a.version()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Stock changed. Refresh before adjusting.");
  if(a.delta()==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quantity change cannot be zero");
  long next=(long)stock.quantity+a.delta();if(next<stock.reserved||next>1000000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Adjustment would reduce stock below reserved quantities or create invalid stock");
  stock.quantity=(int)next;stocks.saveAndFlush(stock);
  var m=new StockMovement();m.requestId=a.requestId();m.stockId=id;m.colour=stock.colour;m.size=stock.size;m.delta=a.delta();m.resultingQuantity=stock.quantity;m.reason=a.reason().trim();m.actor=actor;m.occurredAt=Instant.now();movements.saveAndFlush(m);return stock;
 }
}
