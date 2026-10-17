package com.styledsomehow.backend.shipping;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ShipmentRepository extends JpaRepository<ShipmentBooking,String>{
 @org.springframework.data.jpa.repository.Query("select b.orderId from ShipmentBooking b where b.status = 'BOOKED' and b.trackingTerminal = false and (b.trackingCheckedAt is null or b.trackingCheckedAt <= :before) order by coalesce(b.trackingCheckedAt,b.updatedAt) asc")
 java.util.List<String> trackingQueue(@org.springframework.data.repository.query.Param("before") java.time.Instant before,org.springframework.data.domain.Pageable page);
}
