package com.styledsomehow.backend;
import com.styledsomehow.backend.checkout.*;
import com.styledsomehow.backend.inventory.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.*;
import java.util.concurrent.*;
import static com.styledsomehow.backend.checkout.CheckoutDtos.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest
class CheckoutConcurrencyTests {
 @Autowired CheckoutService checkout;
 @Autowired StockRepository stocks;
 @Autowired OrderRepository orders;
 @Autowired PlatformTransactionManager transactions;
 @Test void twoCheckoutsForLastBlankHaveOnlyOneWinner()throws Exception{
  var tx=new TransactionTemplate(transactions);
  var stock=stocks.findAll().stream().filter(s->s.colour.equals("Black")&&s.size.equals("L")).findFirst().orElseThrow();int original=stock.quantity;
  tx.executeWithoutResult(status->{var s=stocks.locked(stock.id).orElseThrow();s.quantity=1;s.reserved=0;stocks.saveAndFlush(s);});
  var executor=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);var winners=new ConcurrentHashMap<String,String>();
  try{
   List<Future<Boolean>> results=new ArrayList<>();
   for(int i=0;i<2;i++){final String owner="concurrency-"+UUID.randomUUID();results.add(executor.submit(()->{gate.await();var req=new Checkout(List.of(new Item("shes-winning-oversized-t-shirt","Black","L",1)),new Address("Test fixture","test@example.test","9999999999","Test","","Ahmedabad","Gujarat","380054"),UUID.randomUUID().toString(),599);try{var order=checkout.create(req,owner);winners.put(order.id,owner);return true;}catch(org.springframework.web.server.ResponseStatusException e){if(e.getStatusCode().value()!=409)throw e;return false;}}));}
   gate.countDown();int count=0;for(var result:results)if(result.get(20,TimeUnit.SECONDS))count++;assertThat(count).isEqualTo(1);
   assertThat(stocks.findById(stock.id).orElseThrow().reserved).isEqualTo(1);
  }finally{
   executor.shutdownNow();executor.awaitTermination(5,TimeUnit.SECONDS);
   for(var winner:winners.entrySet()){checkout.cancel(winner.getKey(),winner.getValue());orders.deleteById(winner.getKey());}
   tx.executeWithoutResult(status->{var s=stocks.locked(stock.id).orElseThrow();s.quantity=original;s.reserved=0;stocks.saveAndFlush(s);});
  }
 }
}
