package com.styledsomehow.backend.shipping;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ShipmentRepository extends JpaRepository<ShipmentBooking,String>{}
