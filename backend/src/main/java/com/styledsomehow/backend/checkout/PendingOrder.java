package com.styledsomehow.backend.checkout;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="pending_orders")
public class PendingOrder {
 @Id @Column(length=36) public String id;
 @JsonIgnore @Column(length=64) public String sessionKey;
 @JsonIgnore @Column(length=36) public String requestId;
 @JsonIgnore @Column(length=64) public String requestFingerprint;
 public String status;
 @JsonIgnore @Column(length=100,unique=true) public String gatewayOrderId;
 @Column(length=100) public String paymentId;
 public String paymentMode;
 @JsonIgnore @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP) public Instant paymentCheckedAt;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP) public Instant paidAt;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP) public Instant createdAt;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP) public Instant expiresAt;
 public int subtotal;
 public int shipping;
 public int total;
 public String name;
 public String email;
 public String phone;
 public String line1;
 public String line2;
 public String city;
 public String state;
 public String pinCode;
 @ElementCollection @CollectionTable(name="pending_order_lines",joinColumns=@JoinColumn(name="order_id")) @OrderColumn(name="position") public List<OrderLine> items=new ArrayList<>();
 @Embeddable public static class OrderLine {
  public Long stockId;
  public String slug;
  public String name;
  public String colour;
  public String size;
  public int quantity;
  public int unitPrice;
  public String image;
 }
}
