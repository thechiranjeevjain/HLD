package dev.portfolio.tracking.repo;
import dev.portfolio.tracking.domain.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ShipmentRepository extends JpaRepository<Shipment,String>{
  List<Shipment> findByOrderIdOrderById(String orderId);
  Optional<Shipment> findByCarrierIgnoreCaseAndTrackingNumber(String carrier,String trackingNumber);
}
