package com.styledsomehow.backend.checkout;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface OrderRepository extends JpaRepository<PendingOrder,String> {
 Optional<PendingOrder> findBySessionKeyAndRequestId(String sessionKey,String requestId);
 List<PendingOrder> findByStatus(String status);
 boolean existsBySessionKeyAndStatus(String sessionKey,String status);
 List<PendingOrder> findTop100ByOrderByCreatedAtDesc();
}
