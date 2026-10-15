package com.styledsomehow.backend.shipping;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="shipment_bookings")
public class ShipmentBooking {
 @Id @Column(length=36) public String orderId;
 @Column(nullable=false,length=30) public String status;
 public int courierId;
 @Column(nullable=false,length=150) public String courier;
 public int quotedCost;
 public Long providerOrderId;
 @Column(unique=true) public Long providerShipmentId;
 @Column(length=100,unique=true) public String awb;
 @Column(length=500) public String message;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP) public Instant createdAt;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP) public Instant updatedAt;
}
