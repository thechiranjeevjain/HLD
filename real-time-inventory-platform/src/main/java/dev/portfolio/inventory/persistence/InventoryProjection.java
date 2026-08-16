package dev.portfolio.inventory.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "inventory_projection", uniqueConstraints = @UniqueConstraint(columnNames = {"sku", "store_id"}),
       indexes = @Index(name = "idx_inventory_sku", columnList = "sku"))
public class InventoryProjection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String sku;
    @Column(name = "store_id", nullable = false) private String storeId;
    @Column(nullable = false) private long quantity;
    @Column(name = "source_version", nullable = false) private long sourceVersion;
    @Column(name = "event_time", nullable = false) private Instant eventTime;
    @Column(name = "source_update_id", nullable = false) private String sourceUpdateId;
    @Version private long rowVersion;

    protected InventoryProjection() {}
    public InventoryProjection(String sku, String storeId) { this.sku = sku; this.storeId = storeId; }
    public void apply(long quantity, long sourceVersion, Instant eventTime, String sourceUpdateId) {
        this.quantity = quantity; this.sourceVersion = sourceVersion; this.eventTime = eventTime;
        this.sourceUpdateId = sourceUpdateId;
    }
    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getStoreId() { return storeId; }
    public long getQuantity() { return quantity; }
    public long getSourceVersion() { return sourceVersion; }
    public Instant getEventTime() { return eventTime; }
    public String getSourceUpdateId() { return sourceUpdateId; }
}
