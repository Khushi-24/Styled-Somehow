package com.styledsomehow.backend.shipping;
import java.util.List;
import jakarta.validation.constraints.*;
public interface ShippingGateway {
 record Parcel(@DecimalMin("0.001") @DecimalMax("50") double weightKg,@DecimalMin("0.1") @DecimalMax("300") double lengthCm,@DecimalMin("0.1") @DecimalMax("300") double breadthCm,@DecimalMin("0.1") @DecimalMax("300") double heightCm){
  public boolean valid(){return Double.isFinite(weightKg)&&Double.isFinite(lengthCm)&&Double.isFinite(breadthCm)&&Double.isFinite(heightCm)&&weightKg>=0.001&&weightKg<=50&&lengthCm>=0.1&&lengthCm<=300&&breadthCm>=0.1&&breadthCm<=300&&heightCm>=0.1&&heightCm<=300;}
 }
 record Rate(int courierId,String courier,int rupees,Integer transitDays,Double rating){
  public Rate(int courierId,String courier,int rupees){this(courierId,courier,rupees,null,null);}
  public Rate(int courierId,String courier,int rupees,Integer transitDays){this(courierId,courier,rupees,transitDays,null);}
 }
 Rate rate(String pin,int quantity,int subtotal);
 default List<Rate> options(String pin,int quantity,int subtotal,Parcel parcel){return List.of(rate(pin,quantity,subtotal));}
 record DiagnosticCourier(int courierId,String courier,String rate,String freightCharge,String codCharges,String coverageCharges,String chargeWeight,String rating,int blocked){}
 record Diagnostic(long shiprocketOrderId,String pickupPin,String deliveryPin,List<DiagnosticCourier> pinBased,List<DiagnosticCourier> orderBased,java.util.Map<String,String> requestDetails,java.util.Map<String,String> orderDetails,String detailsNotice){
  public Diagnostic(long shiprocketOrderId,String pickupPin,String deliveryPin,List<DiagnosticCourier> pinBased,List<DiagnosticCourier> orderBased){this(shiprocketOrderId,pickupPin,deliveryPin,pinBased,orderBased,java.util.Map.of(),java.util.Map.of(),"");}
 }
 default Diagnostic diagnose(String pin,int quantity,int subtotal,Parcel parcel,long shiprocketOrderId){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Diagnostics unavailable for this shipping provider.");}
}
