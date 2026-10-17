package com.styledsomehow.backend;
import com.styledsomehow.backend.address.PostalLocationService;
import com.styledsomehow.backend.checkout.CheckoutDtos.Address;
import com.styledsomehow.backend.shipping.ShiprocketClient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class PostalLocationTests {
 private Address address(String city,String state){return new Address("Fixture","fixture@gmail.com","9999999999","House and street","",city,state,"362720");}
 @Test void localityAndPostalDistrictAreBothAcceptedButAhmedabadIsRejected() throws Exception {
  var api=mock(ShiprocketClient.class);
  when(api.get(anyString(),anyMap())).thenReturn(new JsonMapper().readTree("{\"success\":true,\"postcode_details\":{\"postcode\":\"362720\",\"city\":\"Gir Somnath\",\"state\":\"Gujarat\",\"locality\":[\"Kodinar\",\"Kodinar\"]}}"));
  var service=new PostalLocationService(api);var location=service.lookup("362720");assertThat(location.towns()).containsExactly("Gir Somnath","Kodinar");
  service.validate(address(" Kodinar "," GUJARAT "));service.validate(address("Gir Somnath","Gujarat"));
  assertThatThrownBy(()->service.validate(address("Ahmedabad","Gujarat"))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("doesn’t match");
  assertThatThrownBy(()->service.validate(address("Kodinar","Maharashtra"))).isInstanceOf(ResponseStatusException.class);
  verify(api,times(1)).get("/open/postcode/details",Map.of("postcode","362720"));
 }
 @Test void mismatchedPinMalformedResponseAndOutageNeverValidateAnAddress() throws Exception {
  var api=mock(ShiprocketClient.class);var service=new PostalLocationService(api);
  when(api.get(anyString(),anyMap())).thenReturn(new JsonMapper().readTree("{\"success\":true,\"postcode_details\":{\"postcode\":\"380015\",\"city\":\"Ahmedabad\",\"state\":\"Gujarat\"}}"));
  assertThatThrownBy(()->service.validate(address("Ahmedabad","Gujarat"))).isInstanceOf(ResponseStatusException.class);
  when(api.get(anyString(),anyMap())).thenReturn(new JsonMapper().readTree("{}"));assertThatThrownBy(()->service.lookup("362720")).isInstanceOf(ResponseStatusException.class);
  when(api.get(anyString(),anyMap())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));assertThatThrownBy(()->service.lookup("362720")).isInstanceOf(ResponseStatusException.class);
  assertThatThrownBy(()->service.lookup("123")).isInstanceOf(ResponseStatusException.class);
 }
}
