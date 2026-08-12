package dev.interview.orders.order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> { Optional<CustomerOrder> findByIdempotencyKey(String key); }
