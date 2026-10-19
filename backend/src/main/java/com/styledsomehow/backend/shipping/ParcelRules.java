package com.styledsomehow.backend.shipping;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
/** Defaults from the owner's 11 x 13 inch pack and 0.5 kg per two tees; verify actual packed parcels. */
@Component
public class ParcelRules {
 public static final int MAX_ITEMS=20;
 public ShippingGateway.Parcel parcel(int quantity){
  if(quantity<1||quantity>MAX_ITEMS)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Please order between 1 and 20 T-shirts per parcel.");
  return new ShippingGateway.Parcel(Math.ceil(quantity/2.0)*0.5,27.94,33.02,quantity*2.54);
 }
}
