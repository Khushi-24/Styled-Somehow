package com.styledsomehow.backend.fulfilment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FulfilmentEventRepository extends JpaRepository<FulfilmentEvent,String>{
 List<FulfilmentEvent> findByOrderIdOrderByOccurredAtAsc(String orderId);
}
