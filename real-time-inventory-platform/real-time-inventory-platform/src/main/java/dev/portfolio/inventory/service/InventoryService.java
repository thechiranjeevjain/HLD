package dev.portfolio.inventory.service;

import dev.portfolio.inventory.api.*;
import dev.portfolio.inventory.persistence.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class InventoryService {
    private final InventoryRepository inventory;
    private final ProcessedUpdateRepository processed;
    private final Counter applied;
    private final Counter stale;
    private final Counter duplicates;
    private final Clock clock = Clock.systemUTC();

    public InventoryService(InventoryRepository inventory, ProcessedUpdateRepository processed, MeterRegistry meters) {
        this.inventory = inventory; this.processed = processed;
        this.applied = meters.counter("inventory_updates_total", "result", "applied");
        this.stale = meters.counter("inventory_updates_total", "result", "stale");
        this.duplicates = meters.counter("inventory_updates_total", "result", "duplicate");
    }

    @Transactional
    public UpdateResult apply(InventoryUpdateRequest update) {
        if (processed.existsById(update.updateId())) {
            duplicates.increment();
            return new UpdateResult(UpdateResult.Status.DUPLICATE_IGNORED, currentOrNull(update.sku(), update.storeId()));
        }
        try {
            processed.saveAndFlush(new ProcessedUpdate(update.updateId(), Instant.now(clock)));
        } catch (DataIntegrityViolationException concurrentDuplicate) {
            duplicates.increment();
            return new UpdateResult(UpdateResult.Status.DUPLICATE_IGNORED, currentOrNull(update.sku(), update.storeId()));
        }

        InventoryProjection state = inventory.findBySkuAndStoreId(update.sku(), update.storeId())
                .orElseGet(() -> new InventoryProjection(update.sku(), update.storeId()));
        if (state.getId() != null && !isNewer(update, state)) {
            stale.increment();
            return new UpdateResult(UpdateResult.Status.STALE_IGNORED, toView(state));
        }
        state.apply(update.quantity(), update.version(), update.eventTime(), update.updateId());
        inventory.save(state);
        applied.increment();
        return new UpdateResult(UpdateResult.Status.APPLIED, toView(state));
    }

    @Transactional(readOnly = true)
    public InventoryView get(String sku, String storeId) {
        return inventory.findBySkuAndStoreId(sku, storeId).map(this::toView)
                .orElseThrow(() -> new NoSuchElementException("No inventory for sku=" + sku + ", store=" + storeId));
    }

    @Transactional(readOnly = true)
    public List<InventoryView> storesForSku(String sku) {
        return inventory.findBySkuOrderByStoreId(sku).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public SkuSummary summary(String sku) {
        List<InventoryView> stores = storesForSku(sku);
        return new SkuSummary(sku, stores.stream().mapToLong(InventoryView::quantity).sum(), stores.size(), stores);
    }

    private boolean isNewer(InventoryUpdateRequest u, InventoryProjection p) {
        if (u.version() != p.getSourceVersion()) return u.version() > p.getSourceVersion();
        int time = u.eventTime().compareTo(p.getEventTime());
        return time > 0 || (time == 0 && u.updateId().compareTo(p.getSourceUpdateId()) > 0);
    }
    private InventoryView currentOrNull(String sku, String storeId) {
        return inventory.findBySkuAndStoreId(sku, storeId).map(this::toView).orElse(null);
    }
    private InventoryView toView(InventoryProjection p) {
        return new InventoryView(p.getSku(), p.getStoreId(), p.getQuantity(), p.getSourceVersion(), p.getEventTime(), p.getSourceUpdateId());
    }

    public record SkuSummary(String sku, long totalQuantity, int reportingStores, List<InventoryView> stores) {}
}
