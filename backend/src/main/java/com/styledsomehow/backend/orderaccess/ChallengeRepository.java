package com.styledsomehow.backend.orderaccess;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
public interface ChallengeRepository extends JpaRepository<AccessChallenge,String> {
 long countByCreatedAtAfter(Instant since);
 long countByEmailHashAndCreatedAtAfter(String hash,Instant since);
 long countByIpHashAndCreatedAtAfter(String hash,Instant since);
 long countByBrowserHashAndCreatedAtAfter(String hash,Instant since);
 long deleteByCreatedAtBefore(Instant before);
}
