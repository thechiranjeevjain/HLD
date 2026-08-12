package dev.interview.orders.order;
import com.fasterxml.jackson.core.JsonProcessingException; import com.fasterxml.jackson.databind.ObjectMapper; import dev.interview.orders.audit.*; import dev.interview.orders.outbox.*; import org.springframework.cache.annotation.*; import org.springframework.dao.DataIntegrityViolationException; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.*;
@Service public class OrderService {
 private final OrderRepository orders; private final OutboxRepository outbox; private final AuditRepository audit; private final ObjectMapper json;
 public OrderService(OrderRepository orders,OutboxRepository outbox,AuditRepository audit,ObjectMapper json){this.orders=orders;this.outbox=outbox;this.audit=audit;this.json=json;}
 @Transactional @CachePut(cacheNames="orders",key="#result.id()")
 public OrderDtos.OrderView create(String customerId,String key,OrderDtos.CreateOrder request){
  var prior=orders.findByIdempotencyKey(key); if(prior.isPresent()){if(!prior.get().getCustomerId().equals(customerId))throw new IllegalArgumentException("idempotency key belongs to another customer");return OrderDtos.OrderView.from(prior.get());}
  var order=new CustomerOrder(UUID.randomUUID(),customerId,request.sku(),request.quantity(),request.unitPrice(),key);
  try{orders.saveAndFlush(order);}catch(DataIntegrityViolationException e){return OrderDtos.OrderView.from(orders.findByIdempotencyKey(key).orElseThrow());}
  outbox.save(new OutboxEvent(UUID.randomUUID(),"Order",order.getId(),"OrderCreated",payload(order)));
  audit.save(new AuditRecord(customerId,"CREATE","Order",order.getId().toString())); return OrderDtos.OrderView.from(order);
 }
 @Transactional(readOnly=true) @Cacheable(cacheNames="orders",key="#id") public OrderDtos.OrderView get(UUID id,String actor,boolean support){var order=orders.findById(id).orElseThrow(()->new NoSuchElementException("order not found"));if(!support&&!order.getCustomerId().equals(actor))throw new SecurityException("not your order");return OrderDtos.OrderView.from(order);}
 private String payload(CustomerOrder o){try{return json.writeValueAsString(Map.of("eventId",UUID.randomUUID(),"orderId",o.getId(),"customerId",o.getCustomerId(),"sku",o.getSku(),"quantity",o.getQuantity(),"occurredAt",java.time.Instant.now()));}catch(JsonProcessingException e){throw new IllegalStateException(e);}}
}
