package com.styledsomehow.backend.operations;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="test_order_deletions")
public class TestOrderDeletion {
 @Id @Column(length=36) public String orderId;
 @Column(length=30,nullable=false) public String previousStatus;
 @Column(length=100,nullable=false) public String actor;
 @Column(length=400,nullable=false) public String reason;
 @Column(nullable=false) public Instant deletedAt;
 public boolean stockRestored;
 public int restoredUnits;
 public int releasedUnits;
}
