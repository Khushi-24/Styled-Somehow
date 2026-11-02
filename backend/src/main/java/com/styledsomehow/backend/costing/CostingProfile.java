package com.styledsomehow.backend.costing;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="costing_profiles")
public class CostingProfile {
 @Id public Long productId;
 @Version public Long version;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal customerShipping=BigDecimal.ZERO;
 public boolean allocateMonthly;
 public Instant updatedAt;
 @ElementCollection @CollectionTable(name="costing_items",joinColumns=@JoinColumn(name="product_id")) @OrderColumn(name="position") public List<Item> items=new ArrayList<>();
 @Embeddable public static class Item {
  @Column(name="cost_key",length=30,nullable=false) public String key;
  @Column(precision=12,scale=2) public BigDecimal amount;
  @Column(nullable=false) public boolean included;
  public Item(){}
  public Item(CostingDtos.Entry e){key=e.key().name();amount=e.amount();included=e.included();}
 }
}
