package com.styledsomehow.backend.catalog;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@RestController
@Transactional
public class ProductController {
 private final ProductRepository products;
 private final com.styledsomehow.backend.inventory.InventoryService inventory;
 public ProductController(ProductRepository products,com.styledsomehow.backend.inventory.InventoryService inventory){this.products=products;this.inventory=inventory;}
 @GetMapping("/api/products") public List<Product> published(){var availability=inventory.availability();return all().stream().filter(p->p.status==Product.Status.PUBLISHED).peek(p->p.availability=availability).toList();}
 @GetMapping("/api/products/{slug}") public Product one(@PathVariable String slug){var p=products.findBySlug(slug).filter(v->v.status==Product.Status.PUBLISHED).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));p.availability=inventory.availability();return p;}
 @GetMapping("/api/admin/products") public List<Product> all(){return products.findAll().stream().sorted(Comparator.comparingInt((Product p)->p.sortOrder).thenComparing(p->p.id)).toList();}
 @PostMapping("/api/admin/products") @ResponseStatus(HttpStatus.CREATED) public Product create(@Valid @RequestBody Product p){
  if(p.id!=null||p.version!=null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"New product cannot have an ID or version");
  checkSlug(p);return products.saveAndFlush(p);
 }
 @PutMapping("/api/admin/products/{id}") public Product update(@PathVariable Long id,@Valid @RequestBody Product p){
  Product current=products.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(!id.equals(p.id)||!Objects.equals(current.version,p.version)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Product changed. Reload before saving.");
  if(!current.slug.equals(p.slug)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Existing product URL cannot change");
  checkSlug(p);return products.saveAndFlush(p);
 }
 private void checkSlug(Product p){if(products.findBySlug(p.slug).filter(existing->!Objects.equals(existing.id,p.id)).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"This product URL already exists");}
}
