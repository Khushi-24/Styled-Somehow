package com.styledsomehow.backend.inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MovementRepository extends JpaRepository<StockMovement,Long> {
 Optional<StockMovement> findByRequestId(String requestId);
 List<StockMovement> findTop100ByOrderByIdDesc();
}
