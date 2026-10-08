package com.styledsomehow.backend.inventory;
import jakarta.persistence.*;
@Entity @Table(name="blank_stock",uniqueConstraints=@UniqueConstraint(columnNames={"colour","size"}))
public class BlankStock {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Version public Long version;
 public String colour;
 public String size;
 public int quantity;
 public int lowStockThreshold;
}
