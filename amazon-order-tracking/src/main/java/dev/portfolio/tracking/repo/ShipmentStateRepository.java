package dev.portfolio.tracking.repo;
import dev.portfolio.tracking.domain.ShipmentState;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ShipmentStateRepository extends JpaRepository<ShipmentState,String>{}
