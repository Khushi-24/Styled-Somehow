package com.styledsomehow.backend.operations;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="commerce_control")
public class CommerceControl {
 @Id public Long id;
 public boolean ordersPaused;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP) public Instant updatedAt;
 @Column(length=100) public String updatedBy;
}
