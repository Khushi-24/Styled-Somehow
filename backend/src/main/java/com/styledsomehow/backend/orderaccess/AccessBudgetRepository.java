package com.styledsomehow.backend.orderaccess;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface AccessBudgetRepository extends JpaRepository<AccessBudget,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select b from AccessBudget b where b.id=1") AccessBudget lockBudget();
}
