package dev.interview.orders.outbox;
import org.springframework.data.jpa.repository.*; import jakarta.persistence.LockModeType; import java.util.*;
public interface OutboxRepository extends JpaRepository<OutboxEvent,UUID>{@Lock(LockModeType.PESSIMISTIC_WRITE) List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc(); long countByPublishedAtIsNull();}
