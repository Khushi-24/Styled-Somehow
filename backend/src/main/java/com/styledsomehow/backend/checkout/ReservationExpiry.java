package com.styledsomehow.backend.checkout;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.*;
@Component @EnableScheduling
public class ReservationExpiry {
 private final CheckoutService checkout;
 public ReservationExpiry(CheckoutService checkout){this.checkout=checkout;}
 @Scheduled(fixedDelay=60000) public void expire(){checkout.expireReservations();}
}
