package com.styledsomehow.backend.costing;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static com.styledsomehow.backend.costing.CostingDtos.*;
class CostingCalculatorTests {
 private Profile costs(boolean overhead){return new Profile(-1,Arrays.stream(Key.values()).map(k->new Entry(k,new BigDecimal("10.00"),true)).toList(),BigDecimal.ZERO,overhead);}
 private Month budget(){return new Month(-1,new BigDecimal("100"),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,3);}
 @Test void fixedBlankAndOptionalCostsUseSellingPriceRatherThanMrp(){var e=CostingCalculator.estimate(599,costs(false),budget());assertThat(e.directCost()).isEqualByComparingTo("370");assertThat(e.profit()).isEqualByComparingTo("229");assertThat(e.margin()).isEqualByComparingTo("38.23");}
 @Test void unselectedItemsDoNotCountAndTheirStoredValuesAreRetained(){var entries=new ArrayList<>(costs(false).items());entries.set(0,new Entry(Key.OUTER_PACKAGING,new BigDecimal("10"),false));var p=new Profile(-1,entries,new BigDecimal("60"),false);var e=CostingCalculator.estimate(599,p,budget());assertThat(e.revenue()).isEqualByComparingTo("659");assertThat(e.directCost()).isEqualByComparingTo("360");assertThat(e.profit()).isEqualByComparingTo("299");assertThat(p.items().getFirst().amount()).isEqualByComparingTo("10");}
 @Test void missingSelectedCostsNeverProduceAnInflatedProfit(){var entries=new ArrayList<>(costs(false).items());entries.set(0,new Entry(Key.OUTER_PACKAGING,null,true));var e=CostingCalculator.estimate(599,new Profile(-1,entries,BigDecimal.ZERO,false),budget());assertThat(e.complete()).isFalse();assertThat(e.profit()).isNull();assertThat(e.missing()).containsExactly("OUTER_PACKAGING");entries.set(0,new Entry(Key.OUTER_PACKAGING,null,false));assertThat(CostingCalculator.estimate(599,new Profile(-1,entries,BigDecimal.ZERO,false),budget()).complete()).isTrue();}
 @Test void overheadIsOptionalRoundedAndMustHaveASalesVolume(){var e=CostingCalculator.estimate(599,costs(true),budget());assertThat(e.overhead()).isEqualByComparingTo("33.33");assertThat(e.profit()).isEqualByComparingTo("195.67");var invalid=new Month(-1,BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,0);assertThat(CostingCalculator.estimate(599,costs(true),invalid).profit()).isNull();assertThat(CostingCalculator.estimate(599,costs(false),invalid).profit()).isEqualByComparingTo("229");}
 @Test void lossesAreShownWithoutClampingToZero(){var e=CostingCalculator.estimate(300,costs(false),budget());assertThat(e.profit()).isEqualByComparingTo("-70");assertThat(e.margin()).isEqualByComparingTo("-23.33");}
}
