package com.styledsomehow.backend.inventory;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface StockRepository extends JpaRepository<BlankStock,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from BlankStock s order by s.id") java.util.List<BlankStock> lockedAll();
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from BlankStock s where s.id=:id") Optional<BlankStock> locked(Long id);
}
