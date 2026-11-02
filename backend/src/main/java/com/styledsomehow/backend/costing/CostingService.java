package com.styledsomehow.backend.costing;
import com.styledsomehow.backend.catalog.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.styledsomehow.backend.costing.CostingDtos.*;
@Service @Transactional(readOnly=true)
public class CostingService {
 private final ProductRepository products;private final EntityManager em;
 public CostingService(ProductRepository products,EntityManager em){this.products=products;this.em=em;}
 private String month(String value){try{if(value==null||!value.matches("[0-9]{4}-[0-9]{2}")||Integer.parseInt(value.substring(0,4))<2000)throw new IllegalArgumentException();return YearMonth.parse(value).toString();}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a valid month (YYYY-MM, year 2000 or later).");}}
 private Month budget(CostingMonth m){return m==null?new Month(-1,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,0):new Month(m.version,m.electricity,m.website,m.ai,m.other,m.expectedUnits);}
 private Profile profile(CostingProfile p){return p==null?new Profile(-1,Arrays.stream(Key.values()).map(k->new Entry(k,null,true)).toList(),BigDecimal.ZERO,false):new Profile(p.version,p.items.stream().map(i->new Entry(Key.valueOf(i.key),i.amount,i.included)).toList(),p.customerShipping,p.allocateMonthly);}
 private Row row(Product p,CostingProfile c,Month m){var dto=profile(c);return new Row(p.id,p.name,p.slug,p.status.name(),p.media.stream().filter(i->"image".equals(i.kind)).map(i->i.url).findFirst().orElse(null),p.price,dto,CostingCalculator.estimate(p.price,dto,m));}
 public Dashboard dashboard(String value){String key=month(value);var m=budget(em.find(CostingMonth.class,key));var profiles=em.createQuery("select p from CostingProfile p",CostingProfile.class).getResultList().stream().collect(Collectors.toMap(p->p.productId,Function.identity()));return new Dashboard(key,CostingCalculator.TSHIRT_COST,m,products.findAll().stream().sorted(Comparator.comparing(p->p.name)).map(p->row(p,profiles.get(p.id),m)).toList());}
 @Transactional public Row save(long id,String value,Profile input){
  String key=month(value);
  if(input.items().stream().map(Entry::key).distinct().count()!=Key.values().length)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Include each costing item exactly once.");
  var product=em.find(Product.class,id,LockModeType.PESSIMISTIC_WRITE);if(product==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found.");
  var p=em.find(CostingProfile.class,id);
  if((p==null?-1:p.version)!=input.version())throw stale();
  if(p==null){p=new CostingProfile();p.productId=id;em.persist(p);}
  p.items.clear();
  for(var item:input.items().stream().sorted(Comparator.comparingInt(i->i.key().ordinal())).toList())p.items.add(new CostingProfile.Item(item));
  p.customerShipping=input.customerShipping();p.allocateMonthly=input.allocateMonthly();p.updatedAt=Instant.now();em.flush();
  return row(product,p,budget(em.find(CostingMonth.class,key)));
 }
 @Transactional public Month saveMonth(String value,Month input){
  String key=month(value);var m=em.find(CostingMonth.class,key,LockModeType.PESSIMISTIC_WRITE);
  if((m==null?-1:m.version)!=input.version())throw stale();
  if(m==null){m=new CostingMonth();m.month=key;em.persist(m);}
  m.electricity=input.electricity();m.website=input.website();m.ai=input.ai();m.other=input.other();m.expectedUnits=input.expectedUnits();em.flush();return budget(m);
 }
 private static ResponseStatusException stale(){return new ResponseStatusException(HttpStatus.CONFLICT,"Costing changed in another tab. Reload before saving; keep a copy of your edits.");}
}
