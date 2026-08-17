package com.example.capstone.ecommerce.inventory;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public InventoryResponse upsert(@Valid @RequestBody UpsertInventoryRequest request) {
        return inventoryService.upsert(request);
    }

    @GetMapping
    public List<InventoryResponse> list() {
        return inventoryService.list();
    }
}
