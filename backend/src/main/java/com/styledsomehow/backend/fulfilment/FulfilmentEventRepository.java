package com.styledsomehow.backend.fulfilment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FulfilmentEventRepository extends JpaRepository<FulfilmentEvent,String>{
 @org.springframework.data.jpa.repository.Query("select max(e.occurredAt) from FulfilmentEvent e where e.orderId = :orderId")
 java.time.Instant latestTime(@org.springframework.data.repository.query.Param("orderId") String orderId);
 @org.springframework.data.jpa.repository.Query("select e from FulfilmentEvent e where e.notificationEligible = true and e.notificationSentAt is null order by e.occurredAt asc")
 List<FulfilmentEvent> mailBacklog(org.springframework.data.domain.Pageable page);
 boolean existsByOrderIdAndStageAndNotificationEligibleTrue(String orderId,String stage);
 @org.springframework.data.jpa.repository.Query("select e.id from FulfilmentEvent e, PendingOrder o where e.orderId = o.id and e.notificationEligible = true and e.notificationSentAt is null and (e.notificationNextAttemptAt is null or e.notificationNextAttemptAt <= :now) and o.status = 'PAID' and o.confirmationSentAt is not null and not exists (select earlier.id from FulfilmentEvent earlier where earlier.orderId = e.orderId and earlier.notificationEligible = true and earlier.notificationSentAt is null and (earlier.occurredAt < e.occurredAt or (earlier.occurredAt = e.occurredAt and earlier.id < e.id))) order by e.occurredAt asc, e.id asc")
 List<String> notificationQueue(@org.springframework.data.repository.query.Param("now") java.time.Instant now,org.springframework.data.domain.Pageable page);
 @org.springframework.data.jpa.repository.Query("select count(earlier) from FulfilmentEvent earlier where earlier.orderId = :orderId and earlier.notificationEligible = true and earlier.notificationSentAt is null and (earlier.occurredAt < :occurredAt or (earlier.occurredAt = :occurredAt and earlier.id < :id))")
 long pendingPredecessors(@org.springframework.data.repository.query.Param("orderId") String orderId,@org.springframework.data.repository.query.Param("occurredAt") java.time.Instant occurredAt,@org.springframework.data.repository.query.Param("id") String id);

 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select e from FulfilmentEvent e where e.id = :id")
 java.util.Optional<FulfilmentEvent> lockMail(@org.springframework.data.repository.query.Param("id") String id);
 List<FulfilmentEvent> findByOrderIdOrderByOccurredAtAsc(String orderId);
}
