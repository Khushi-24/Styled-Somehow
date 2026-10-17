package com.styledsomehow.backend.fulfilment;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="fulfilment_events")
public class FulfilmentEvent {
 @Id @Column(length=36) public String id;
 @Column(nullable=false,length=36) public String orderId;
 @Column(nullable=false,length=30) public String stage;
 @Column(nullable=false,length=100) public String actor;
 @Column(nullable=false,length=500) public String note;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP) public Instant occurredAt;
}
