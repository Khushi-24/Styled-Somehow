package com.styledsomehow.backend;
import com.styledsomehow.backend.catalog.ApiErrors;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
class ApiErrorsTests {
 @Test void shippingRejectionPreservesStatusAndCustomerMessage(){
  var response=new ApiErrors().status(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Delhivery delivery is unavailable for this PIN code."));
  assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
  assertThat(response.getBody()).containsEntry("message","Delhivery delivery is unavailable for this PIN code.").hasSize(1);
 }
 @Test void missingReasonStillReturnsReadableMessage(){
  assertThat(new ApiErrors().status(new ResponseStatusException(HttpStatus.NOT_FOUND)).getBody()).containsEntry("message","The request could not be completed. Please try again.");
 }
}
