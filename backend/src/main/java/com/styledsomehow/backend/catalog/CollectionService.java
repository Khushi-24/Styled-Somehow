package com.styledsomehow.backend.catalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@Service
@Transactional
public class CollectionService {
 private final CollectionRepository collections;
 private final ProductRepository products;
 public CollectionService(CollectionRepository collections,ProductRepository products){this.collections=collections;this.products=products;}
 public record View(String slug,String name,int sortOrder,long productCount){}
 @Transactional(readOnly=true) public List<View> list(boolean admin){
  var all=products.findAll();
  return collections.findByActiveTrueOrderBySortOrderAscSlugAsc().stream().map(c->new View(c.slug,c.name,c.sortOrder,all.stream().filter(p->(admin||p.status==Product.Status.PUBLISHED)&&p.collections.contains(c.slug)).count())).toList();
 }
 public View create(String slug,String name){
  var all=collections.lockAll();
  var c=all.stream().filter(v->v.slug.equals(slug)).findFirst().orElse(null);
  if(c!=null&&c.active)throw new ResponseStatusException(HttpStatus.CONFLICT,"This collection URL already exists.");
  if(all.stream().anyMatch(v->v.active&&v.name.equalsIgnoreCase(name.trim())))throw new ResponseStatusException(HttpStatus.CONFLICT,"A collection with this name already exists.");
  if(all.stream().filter(v->v.active).count()>=50)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You can have up to 50 collections.");
  if(c==null){c=new ShopCollection();c.slug=slug;}
  c.name=name.trim();c.active=true;c.sortOrder=all.stream().mapToInt(v->v.sortOrder).max().orElse(0)+1;
  collections.saveAndFlush(c);return new View(c.slug,c.name,c.sortOrder,0);
 }
 public void remove(String slug){
  var c=collections.lockAll().stream().filter(v->v.active&&v.slug.equals(slug)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Collection not found."));
  // Detach memberships, preserving products, orders, media and stock. Hibernate updates product versions.
  for(var p:products.findAll())if(p.collections.contains(slug))p.collections.removeIf(slug::equals);
  c.active=false;products.flush();
 }
 public void validateMemberships(Product product){
  var active=collections.lockAll().stream().filter(c->c.active).map(c->c.slug).toList();
  if(product.collections.stream().anyMatch(c->!active.contains(c)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A selected collection was removed. Refresh collections and choose again.");
 }
}
