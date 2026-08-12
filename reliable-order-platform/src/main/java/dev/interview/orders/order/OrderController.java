package dev.interview.orders.order;
import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.web.bind.annotation.*; import java.net.URI; import java.util.UUID;
@RestController @RequestMapping("/api/v1/orders") public class OrderController {
 private final OrderService service; public OrderController(OrderService service){this.service=service;}
 @PostMapping public ResponseEntity<OrderDtos.OrderView> create(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody OrderDtos.CreateOrder request,Authentication auth){var result=service.create(auth.getName(),key,request);return ResponseEntity.created(URI.create("/api/v1/orders/"+result.id())).body(result);}
 @GetMapping("/{id}") public OrderDtos.OrderView get(@PathVariable UUID id,Authentication auth){return service.get(id,auth.getName(),auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_SUPPORT")));}
}
