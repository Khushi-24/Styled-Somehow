package com.styledsomehow.backend.shipping;
public interface ShippingGateway {
 record Rate(int courierId,String courier,int rupees){}
 Rate rate(String pin,int quantity,int subtotal);
}
