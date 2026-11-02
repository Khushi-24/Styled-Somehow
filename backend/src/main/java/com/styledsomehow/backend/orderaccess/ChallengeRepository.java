package com.styledsomehow.backend.orderaccess;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
public interface ChallengeRepository extends JpaRepository<AccessChallenge,String> {
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select c from AccessChallenge c where c.id = :id")
 java.util.Optional<AccessChallenge> lock(@org.springframework.data.repository.query.Param("id") String id);
 long countByCreatedAtAfter(Instant since);
 long countByEmailHashAndCreatedAtAfter(String hash,Instant since);
 long countByIpHashAndCreatedAtAfter(String hash,Instant since);
 long countByBrowserHashAndCreatedAtAfter(String hash,Instant since);
 long deleteByCreatedAtBefore(Instant before);
}
