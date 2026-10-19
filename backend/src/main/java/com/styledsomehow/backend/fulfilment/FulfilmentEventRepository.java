package com.styledsomehow.backend.fulfilment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FulfilmentEventRepository extends JpaRepository<FulfilmentEvent,String>{
 boolean existsByOrderIdAndStageAndNotificationEligibleTrue(String orderId,String stage);
 @org.springframework.data.jpa.repository.Query("select e.id from FulfilmentEvent e, PendingOrder o where e.orderId = o.id and e.notificationEligible = true and e.notificationSentAt is null and (e.notificationNextAttemptAt is null or e.notificationNextAttemptAt <= :now) and o.status = 'PAID' and o.confirmationSentAt is not null and o.confirmationAttempts > 0 order by e.occurredAt asc")
 List<String> notificationQueue(@org.springframework.data.repository.query.Param("now") java.time.Instant now,org.springframework.data.domain.Pageable page);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select e from FulfilmentEvent e where e.id = :id")
 java.util.Optional<FulfilmentEvent> lockMail(@org.springframework.data.repository.query.Param("id") String id);
 List<FulfilmentEvent> findByOrderIdOrderByOccurredAtAsc(String orderId);
}
