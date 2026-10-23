package com.styledsomehow.backend.costing;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="costing_months")
public class CostingMonth {
 @Id @Column(name="month_key",length=7) public String month;
 @Version public Long version;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal electricity=BigDecimal.ZERO;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal website=BigDecimal.ZERO;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal ai=BigDecimal.ZERO;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal other=BigDecimal.ZERO;
 public int expectedUnits;
}
