package com.styledsomehow.backend.shipping;
public interface ShippingGateway {
 record Rate(int courierId,String courier,int rupees,Integer transitDays){
  public Rate(int courierId,String courier,int rupees){this(courierId,courier,rupees,null);}
 }
 Rate rate(String pin,int quantity,int subtotal);
}
