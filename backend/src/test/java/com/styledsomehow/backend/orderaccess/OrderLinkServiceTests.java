package com.styledsomehow.backend.orderaccess;
import com.styledsomehow.backend.checkout.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class OrderLinkServiceTests {
 private static final String SECRET="fixture-link-secret-not-for-deployment-12345678";
 private final Clock clock=Clock.fixed(Instant.parse("2026-10-18T00:00:00Z"),ZoneOffset.UTC);
 private PendingOrder order(){var o=new PendingOrder();o.id="7792ba94-2a93-4579-9fe5-ab1b5c0f0362";o.status="PAID";o.email="fixture@gmail.com";o.paymentId="pay_fixture";o.paidAt=clock.instant().minusSeconds(3600);return o;}
 private String token(OrderLinkService service,PendingOrder o){return service.link(o,"https://styledsomehow.in").split("#access=")[1];}
 @Test void linkIsReadOnlyReusableAndBoundToOneOrder(){
  var repo=mock(OrderRepository.class);var o=order();when(repo.findById(o.id)).thenReturn(Optional.of(o));var service=new OrderLinkService(repo,SECRET,clock);
  String value=token(service,o);assertThat(value).doesNotContain(o.email,o.paymentId);
  // Legacy links expired at paidAt + 90 days; retain their signed payload format.
  String legacy=token(new OrderLinkService(repo,SECRET,Clock.fixed(o.paidAt,ZoneOffset.UTC)),o);
  assertThat(service.verify(legacy)).isEqualTo(o.id);
  assertThat(service.verify(value)).isEqualTo(o.id);assertThat(service.verify(value)).isEqualTo(o.id);
  assertThat(service.link(o,"https://styledsomehow.in")).startsWith("https://styledsomehow.in/orders#access=");
 }
 @Test void tamperingExpiredLinksAndSecretRotationFail(){
  var repo=mock(OrderRepository.class);var o=order();when(repo.findById(o.id)).thenReturn(Optional.of(o));var service=new OrderLinkService(repo,SECRET,clock);String value=token(service,o);
  int signatureStart=value.lastIndexOf('.')+1;String tampered=value.substring(0,signatureStart)+(value.charAt(signatureStart)=='A'?"B":"A")+value.substring(signatureStart+1);
  assertThatThrownBy(()->service.verify(tampered)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  assertThatThrownBy(()->new OrderLinkService(repo,SECRET,Clock.offset(clock,Duration.ofDays(91))).verify(value)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  assertThatThrownBy(()->new OrderLinkService(repo,SECRET+"rotated",clock).verify(value)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  assertThatThrownBy(()->service.verify(o.id)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }
 @Test void changingOrderIdentityInvalidatesLink(){
  var repo=mock(OrderRepository.class);var o=order();when(repo.findById(o.id)).thenReturn(Optional.of(o));var service=new OrderLinkService(repo,SECRET,clock);String value=token(service,o);
  o.email="another@gmail.com";assertThatThrownBy(()->service.verify(value)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }
 @Test void missingSecretOrUnpaidOrderCannotMintLinks(){
  var repo=mock(OrderRepository.class);var o=order();assertThatThrownBy(()->new OrderLinkService(repo,"",clock).link(o,"https://styledsomehow.in")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  o.paidAt=null;assertThatThrownBy(()->new OrderLinkService(repo,SECRET,clock).link(o,"https://styledsomehow.in")).isInstanceOf(IllegalStateException.class);
 }
 @Test void oldPurchaseGetsFreshLinkAndPreviouslyIssuedLinksRemainValid(){
  var repo=mock(OrderRepository.class);var o=order();o.paidAt=clock.instant().minus(Duration.ofDays(120));when(repo.findById(o.id)).thenReturn(Optional.of(o));
  var service=new OrderLinkService(repo,SECRET,clock);String fresh=token(service,o);assertThat(service.verify(fresh)).isEqualTo(o.id);
  var earlier=new OrderLinkService(repo,SECRET,Clock.offset(clock,Duration.ofDays(-10)));String existing=token(earlier,o);
  assertThat(service.verify(existing)).isEqualTo(o.id);
  assertThatThrownBy(()->new OrderLinkService(repo,SECRET,Clock.offset(clock,Duration.ofDays(91))).verify(fresh)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }

}
