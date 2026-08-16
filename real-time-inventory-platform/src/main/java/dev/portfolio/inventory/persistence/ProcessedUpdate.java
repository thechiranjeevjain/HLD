package dev.portfolio.inventory.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "processed_update")
public class ProcessedUpdate {
    @Id @Column(name = "update_id") private String updateId;
    @Column(name = "processed_at", nullable = false) private Instant processedAt;
    protected ProcessedUpdate() {}
    public ProcessedUpdate(String updateId, Instant processedAt) { this.updateId = updateId; this.processedAt = processedAt; }
}
