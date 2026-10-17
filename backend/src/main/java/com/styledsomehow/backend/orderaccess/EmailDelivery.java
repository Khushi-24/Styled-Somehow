package com.styledsomehow.backend.orderaccess;
public interface EmailDelivery {
 boolean ready();
 default void sendConfirmation(com.styledsomehow.backend.checkout.PendingOrder order){throw new UnsupportedOperationException("Confirmation delivery unavailable");}
 void sendCode(String email,String code);
 default void sendRecovery(String email,String code,java.util.List<String> references){sendCode(email,code);}
}
