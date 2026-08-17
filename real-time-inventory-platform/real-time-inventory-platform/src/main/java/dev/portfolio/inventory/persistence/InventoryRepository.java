package dev.portfolio.inventory.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryProjection, Long> {
    Optional<InventoryProjection> findBySkuAndStoreId(String sku, String storeId);
    List<InventoryProjection> findBySkuOrderByStoreId(String sku);
}
