package com.styledsomehow.backend.orderaccess;
public interface EmailDelivery {
 boolean ready();
 void sendCode(String email,String code);
}
