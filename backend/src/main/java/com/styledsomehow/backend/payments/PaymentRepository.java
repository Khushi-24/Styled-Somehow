package com.styledsomehow.backend.payments;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PaymentRepository extends JpaRepository<PaymentReceipt,String>{
 List<PaymentReceipt> findTop20ByStatusInOrderByUpdatedAtAsc(List<String> statuses);
 List<PaymentReceipt> findTop100ByOrderByCreatedAtDesc();
}
