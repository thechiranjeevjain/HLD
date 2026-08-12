package dev.interview.orders.outbox;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="processed_events") public class ProcessedEvent {@Id private UUID eventId; @Column(nullable=false) private Instant processedAt; protected ProcessedEvent(){} public ProcessedEvent(UUID id){eventId=id;processedAt=Instant.now();}}
