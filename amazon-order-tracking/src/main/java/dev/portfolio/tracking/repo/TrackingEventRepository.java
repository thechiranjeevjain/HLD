package dev.portfolio.tracking.repo;
import dev.portfolio.tracking.domain.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TrackingEventRepository extends JpaRepository<TrackingEvent,String>{ boolean existsByIdempotencyKey(String key); boolean existsByShipmentIdAndRawPayloadHash(String shipmentId,String hash); List<TrackingEvent> findByShipmentIdOrderByEventTimeAscReceivedTimeAsc(String shipmentId); }
