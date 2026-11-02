package com.styledsomehow.backend.costing;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
public final class CostingDtos {
 private CostingDtos(){}
 public enum Key { OUTER_PACKAGING, INNER_PACKAGING, THANK_YOU_CARD, CLOTH_TAG, FREEBIES, SHIPPING, DTF_PRINT, PAYMENT_FEE, EXTRA }
 public record Entry(@NotNull Key key,@DecimalMin("0") @DecimalMax("1000000") @Digits(integer=7,fraction=2) BigDecimal amount,boolean included){}
 public record Profile(@Min(-1) long version,@NotNull @Size(min=9,max=9) List<@NotNull @Valid Entry> items,@NotNull @DecimalMin("0") @DecimalMax("1000000") @Digits(integer=7,fraction=2) BigDecimal customerShipping,boolean allocateMonthly){}
 public record Month(@Min(-1) long version,@NotNull @DecimalMin("0") @DecimalMax("1000000") @Digits(integer=7,fraction=2) BigDecimal electricity,@NotNull @DecimalMin("0") @DecimalMax("1000000") @Digits(integer=7,fraction=2) BigDecimal website,@NotNull @DecimalMin("0") @DecimalMax("1000000") @Digits(integer=7,fraction=2) BigDecimal ai,@NotNull @DecimalMin("0") @DecimalMax("1000000") @Digits(integer=7,fraction=2) BigDecimal other,@Min(0) @Max(1000000) int expectedUnits){}
 public record Estimate(boolean complete,List<String> missing,BigDecimal revenue,BigDecimal directCost,BigDecimal overhead,BigDecimal totalCost,BigDecimal profit,BigDecimal margin){}
 public record Row(long productId,String name,String slug,String status,String image,int sellingPrice,Profile profile,Estimate estimate){}
 public record Dashboard(String month,BigDecimal fixedTshirtCost,Month budget,List<Row> products){}
}
