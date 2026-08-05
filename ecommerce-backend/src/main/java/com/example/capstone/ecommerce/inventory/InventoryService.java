package com.example.capstone.ecommerce.inventory;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public InventoryResponse upsert(UpsertInventoryRequest request) {
        String sku = normalizeSku(request.sku());
        InventoryItem item = inventoryRepository.findBySku(sku)
                .orElseGet(() -> new InventoryItem(sku, request.name().trim(), request.price(), request.currency(), request.stockQuantity()));
        item.replace(request.name().trim(), request.price(), request.currency(), request.stockQuantity());
        return InventoryResponse.from(inventoryRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> list() {
        return inventoryRepository.findAll().stream()
                .map(InventoryResponse::from)
                .toList();
    }

    public String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }
}
