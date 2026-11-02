package com.styledsomehow.backend.catalog;
import jakarta.persistence.*;
@Entity
@Table(name="shop_collections")
public class ShopCollection {
 @Id @Column(length=100) public String slug;
 @Column(nullable=false,length=100) public String name;
 public boolean active=true;
 public int sortOrder;
}
