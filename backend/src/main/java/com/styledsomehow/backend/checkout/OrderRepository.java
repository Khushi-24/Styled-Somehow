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
 @org.springframework.data.jpa.repository.Query("select o from PendingOrder o where o.status = 'PAID' and o.confirmationSentAt is null order by o.paidAt asc")
 List<PendingOrder> confirmationBacklog(org.springframework.data.domain.Pageable page);
 long countByEmailIgnoreCaseAndStatus(String email,String status);
 List<PendingOrder> findByStatus(String status);
 boolean existsBySessionKeyAndStatus(String sessionKey,String status);
 @org.springframework.data.jpa.repository.Query("select o.id from PendingOrder o where o.status = 'PAID' and o.confirmationSentAt is null and (o.confirmationNextAttemptAt is null or o.confirmationNextAttemptAt <= :now) order by o.paidAt asc")
 List<String> confirmationQueue(@org.springframework.data.repository.query.Param("now") java.time.Instant now,org.springframework.data.domain.Pageable page);
 @org.springframework.data.jpa.repository.Query("select o from PendingOrder o where lower(o.email) = :email and o.paidAt is not null and o.paymentId is not null order by o.createdAt desc, o.id desc")
 org.springframework.data.domain.Slice<PendingOrder> emailHistory(@org.springframework.data.repository.query.Param("email") String email,org.springframework.data.domain.Pageable page);
 @org.springframework.data.jpa.repository.Query("select o from PendingOrder o where :query = '' or lower(o.id) like concat('%',:query,'%') or lower(o.email) like concat('%',:query,'%') or lower(o.name) like concat('%',:query,'%') or o.phone like concat('%',:query,'%') order by o.createdAt desc, o.id desc")
 org.springframework.data.domain.Slice<PendingOrder> adminSearch(@org.springframework.data.repository.query.Param("query") String query,org.springframework.data.domain.Pageable page);
 List<PendingOrder> findTop100ByOrderByCreatedAtDesc();
 List<PendingOrder> findTop20ByEmailIgnoreCaseAndStatusInOrderByCreatedAtDesc(String email,List<String> statuses);
}
