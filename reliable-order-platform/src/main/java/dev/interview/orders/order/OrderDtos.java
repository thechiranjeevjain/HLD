package dev.interview.orders.order;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public final class OrderDtos {
 private OrderDtos(){}
 public record CreateOrder(@NotBlank String sku,@Min(1) @Max(1000) int quantity,@DecimalMin("0.01") BigDecimal unitPrice){}
 public record OrderView(UUID id,String customerId,String sku,int quantity,BigDecimal unitPrice,OrderStatus status,Instant createdAt,long version){static OrderView from(CustomerOrder o){return new OrderView(o.getId(),o.getCustomerId(),o.getSku(),o.getQuantity(),o.getUnitPrice(),o.getStatus(),o.getCreatedAt(),o.getVersion());}}
}
