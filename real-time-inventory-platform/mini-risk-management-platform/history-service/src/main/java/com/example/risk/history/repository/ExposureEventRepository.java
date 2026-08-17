package com.example.risk.history.repository;

import com.example.risk.history.domain.ExposureEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExposureEventRepository extends JpaRepository<ExposureEvent, UUID> {
    boolean existsByOrderId(UUID orderId);

    List<ExposureEvent> findTop50ByClientIdOrderByOccurredAtDesc(String clientId);

    @Query(value = """
            SELECT
                COALESCE(SUM(CASE WHEN side = 'BUY' THEN quantity ELSE -quantity END), 0) AS "netQuantity",
                COALESCE(SUM(notional), 0) AS "grossNotional",
                COALESCE(SUM(CASE WHEN occurred_at >= CURRENT_DATE THEN notional ELSE 0 END), 0) AS "dailyExposure"
            FROM exposure_events
            WHERE client_id = :clientId
              AND symbol = :symbol
              AND status = 'ACCEPTED'
            """, nativeQuery = true)
    ExposureAggregate aggregate(@Param("clientId") String clientId, @Param("symbol") String symbol);
}

