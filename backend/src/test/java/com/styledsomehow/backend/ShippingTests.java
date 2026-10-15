package com.styledsomehow.backend;
import com.styledsomehow.backend.shipping.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
class ShippingTests {
 @Test void onlyUnblockedBlueDartRatesAreSelectedAndRoundedUp() throws Exception {
  var node=new JsonMapper().readTree("[{\"courier_name\":\"Delhivery\",\"courier_company_id\":2,\"rate\":10},{\"courier_name\":\"Blue Dart Air\",\"courier_company_id\":3,\"rate\":84.25},{\"courier_name\":\"BlueDart Surface\",\"courier_company_id\":4,\"rate\":80,\"blocked\":1}]");
  assertThat(ShiprocketGateway.selectRate(node)).isEqualTo(new ShippingGateway.Rate(3,"Blue Dart Air",85));
 }
 @Test void missingOrInvalidBlueDartRatesFailClosed() throws Exception {
  for(String json:new String[]{"[]","[{\"courier_name\":\"Blue Dart\",\"courier_company_id\":1}]","[{\"courier_name\":\"Delhivery\",\"rate\":30}]"})assertThatThrownBy(()->ShiprocketGateway.selectRate(new JsonMapper().readTree(json))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }
}
