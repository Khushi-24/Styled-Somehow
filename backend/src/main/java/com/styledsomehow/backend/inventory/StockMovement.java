package com.styledsomehow.backend.inventory;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="stock_movements")
public class StockMovement {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(unique=true,nullable=false) public String requestId;
 public Long stockId;
 public String colour;
 public String size;
 public int delta;
 public int resultingQuantity;
 @Column(length=500) public String reason;
 public String actor;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP) public Instant occurredAt;
}
