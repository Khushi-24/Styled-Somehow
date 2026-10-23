package com.styledsomehow.backend.costing;
import java.math.*;
import java.util.*;
import static com.styledsomehow.backend.costing.CostingDtos.*;
public final class CostingCalculator {
 public static final BigDecimal TSHIRT_COST=new BigDecimal("280.00");
 private CostingCalculator(){}
 private static BigDecimal money(BigDecimal n){return n.setScale(2,RoundingMode.HALF_UP);}
 public static Estimate estimate(int sellingPrice,Profile profile,Month month){
  var missing=new ArrayList<String>();BigDecimal direct=TSHIRT_COST;
  for(var e:profile.items())if(e.included()){if(e.amount()==null)missing.add(e.key().name());else direct=direct.add(e.amount());}
  BigDecimal overhead=BigDecimal.ZERO;
  if(profile.allocateMonthly()){
   if(month.expectedUnits()<=0)missing.add("EXPECTED_MONTHLY_UNITS");
   else overhead=month.electricity().add(month.website()).add(month.ai()).add(month.other()).divide(BigDecimal.valueOf(month.expectedUnits()),2,RoundingMode.HALF_UP);
  }
  var revenue=money(BigDecimal.valueOf(sellingPrice).add(profile.customerShipping()));
  var total=money(direct.add(overhead));boolean complete=missing.isEmpty();var profit=complete?money(revenue.subtract(total)):null;
  var margin=complete?profit.multiply(BigDecimal.valueOf(100)).divide(revenue,2,RoundingMode.HALF_UP):null;
  return new Estimate(complete,List.copyOf(missing),revenue,money(direct),money(overhead),total,profit,margin);
 }
}
