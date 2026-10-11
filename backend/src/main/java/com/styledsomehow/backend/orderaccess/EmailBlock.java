package com.styledsomehow.backend.orderaccess;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="order_access_email_blocks")
public class EmailBlock {
 @Id @Column(length=64) public String emailHash;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP) public Instant blockedUntil;
}
