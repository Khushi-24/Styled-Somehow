package com.styledsomehow.backend.operations;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
public interface CommerceAttemptRepository extends JpaRepository<CommerceAttempt,String> {
 long countByScopeAndIpHashAndCreatedAtAfter(String scope,String hash,Instant since);
 long countByScopeAndEmailHashAndCreatedAtAfter(String scope,String hash,Instant since);
 long countByScopeAndBrowserHashAndCreatedAtAfter(String scope,String hash,Instant since);
 long countByScopeAndCreatedAtAfter(String scope,Instant since);
 long deleteByCreatedAtBefore(Instant before);
}
