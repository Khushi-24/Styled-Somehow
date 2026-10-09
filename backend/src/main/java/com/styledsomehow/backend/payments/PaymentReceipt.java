package com.styledsomehow.backend.payments;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="payment_receipts")
public class PaymentReceipt {
 @Id @Column(length=100) public String id;
 @Column(length=36) public String orderId;
 public long amount;
 public String status;
 @Column(length=100) public String refundId;
 @Column(length=36) public String refundKey;
 @Column(length=500) public String reason;
 public int retries;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP) public Instant createdAt,updatedAt;
}
