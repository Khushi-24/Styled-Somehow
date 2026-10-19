package com.styledsomehow.backend.operations;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="commerce_attempts")
public class CommerceAttempt {
 @Id @Column(length=36) public String id;
 @Column(length=20) public String scope;
 @Column(length=64) public String ipHash,emailHash,browserHash;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP) public Instant createdAt;
}
