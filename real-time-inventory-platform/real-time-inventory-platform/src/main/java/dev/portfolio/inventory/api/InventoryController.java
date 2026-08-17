package dev.portfolio.inventory.api;

import dev.portfolio.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InventoryController {
    private final InventoryService service;
    public InventoryController(InventoryService service) { this.service = service; }

    @PostMapping("/inventory/updates")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UpdateResult update(@Valid @RequestBody InventoryUpdateRequest request) { return service.apply(request); }

    @PostMapping("/inventory/updates/batch")
    public List<UpdateResult> batch(@RequestBody List<@Valid InventoryUpdateRequest> requests) {
        return requests.stream().map(service::apply).toList();
    }

    @GetMapping("/inventory/{sku}/stores/{storeId}")
    public InventoryView get(@PathVariable String sku, @PathVariable String storeId) { return service.get(sku, storeId); }

    @GetMapping("/inventory/{sku}")
    public List<InventoryView> stores(@PathVariable String sku) { return service.storesForSku(sku); }

    @GetMapping("/inventory/{sku}/summary")
    public InventoryService.SkuSummary summary(@PathVariable String sku) { return service.summary(sku); }
}
