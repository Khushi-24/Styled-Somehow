package com.styledsomehow.backend.orderaccess;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="order_access_challenges")
public class AccessChallenge {
 @Id @Column(length=36) public String id;
 @Column(length=36) public String orderId;
 @Column(length=64) public String emailHash,ipHash,browserHash,codeHash;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP) public Instant createdAt,expiresAt;
 public int attempts;
 public boolean consumed;
}
