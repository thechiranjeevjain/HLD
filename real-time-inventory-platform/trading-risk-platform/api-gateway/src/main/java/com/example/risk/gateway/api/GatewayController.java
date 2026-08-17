package com.example.risk.gateway.api;

import com.example.risk.common.ExposureSummary;
import com.example.risk.common.OrderRequest;
import com.example.risk.common.OrderResponse;
import com.example.risk.gateway.client.PlatformClients;
import jakarta.validation.Valid;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class GatewayController {
    private final PlatformClients clients;

    public GatewayController(PlatformClients clients) {
        this.clients = clients;
    }

    @PostMapping("/orders")
    public OrderResponse submit(@Valid @RequestBody OrderRequest request) {
        return clients.order()
                .post()
                .uri("/orders")
                .body(request)
                .retrieve()
                .body(OrderResponse.class);
    }

    @GetMapping("/orders/{orderId}")
    public Map<String, Object> order(@PathVariable UUID orderId) {
        return clients.order()
                .get()
                .uri("/orders/{orderId}", orderId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @GetMapping("/exposures")
    public List<ExposureSummary> exposures() {
        return clients.history()
                .get()
                .uri("/exposures")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @GetMapping("/exposures/{clientId}/{symbol}")
    public ExposureSummary exposure(@PathVariable String clientId, @PathVariable String symbol) {
        return clients.history()
                .get()
                .uri("/exposures/{clientId}/{symbol}", clientId, symbol)
                .retrieve()
                .body(ExposureSummary.class);
    }

    @GetMapping("/alerts")
    public List<Map<String, Object>> alerts() {
        return clients.notification()
                .get()
                .uri("/alerts")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
