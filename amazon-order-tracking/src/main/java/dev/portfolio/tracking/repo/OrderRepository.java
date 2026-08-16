package dev.portfolio.tracking.repo;
import dev.portfolio.tracking.domain.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderRepository extends JpaRepository<OrderEntity,String>{}
