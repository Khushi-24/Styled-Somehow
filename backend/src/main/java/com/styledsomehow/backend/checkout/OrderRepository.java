package com.styledsomehow.backend.checkout;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface OrderRepository extends JpaRepository<PendingOrder,String> {
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select o from PendingOrder o where o.id = :id")
 Optional<PendingOrder> lockForShipping(@org.springframework.data.repository.query.Param("id") String id);
 Optional<PendingOrder> findByGatewayOrderId(String id);
 @org.springframework.data.jpa.repository.Query("select o from PendingOrder o where o.gatewayOrderId is not null order by coalesce(o.paymentCheckedAt,o.createdAt) asc")
 List<PendingOrder> paymentChecks(org.springframework.data.domain.Pageable page);
 Optional<PendingOrder> findBySessionKeyAndRequestId(String sessionKey,String requestId);
 List<PendingOrder> findByStatus(String status);
 boolean existsBySessionKeyAndStatus(String sessionKey,String status);
 List<PendingOrder> findTop100ByOrderByCreatedAtDesc();
}
