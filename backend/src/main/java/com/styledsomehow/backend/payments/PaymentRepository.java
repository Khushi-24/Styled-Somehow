package com.styledsomehow.backend.payments;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PaymentRepository extends JpaRepository<PaymentReceipt,String>{
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select r from PaymentReceipt r where r.id = :id")
 Optional<PaymentReceipt> lock(@org.springframework.data.repository.query.Param("id") String id);
 List<PaymentReceipt> findTop20ByStatusInOrderByUpdatedAtAsc(List<String> statuses);
 @org.springframework.data.jpa.repository.Query("select r from PaymentReceipt r where :query = '' or lower(r.id) like concat('%',:query,'%') or lower(r.orderId) like concat('%',:query,'%') or lower(r.status) like concat('%',:query,'%') or lower(r.refundId) like concat('%',:query,'%') order by r.createdAt desc, r.id desc")
 org.springframework.data.domain.Slice<PaymentReceipt> search(@org.springframework.data.repository.query.Param("query") String query,org.springframework.data.domain.Pageable page);
 List<PaymentReceipt> findTop100ByOrderByCreatedAtDesc();
}
