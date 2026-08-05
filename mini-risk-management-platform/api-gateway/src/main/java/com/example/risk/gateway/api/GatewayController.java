package com.example.risk.gateway.api;

import com.example.risk.common.ExposureSummary;
import com.example.risk.common.OrderRequest;
import com.example.risk.common.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class GatewayController {
    private final RestClient orderClient;
    private final RestClient historyClient;

    public GatewayController(
            @Qualifier("orderRestClient") RestClient orderClient,
            @Qualifier("historyRestClient") RestClient historyClient
    ) {
        this.orderClient = orderClient;
        this.historyClient = historyClient;
    }

    @PostMapping("/orders")
    ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest request) {
        try {
            OrderResponse response = orderClient.post()
                    .uri("/orders")
                    .body(request)
                    .retrieve()
                    .body(OrderResponse.class);
            return ResponseEntity.accepted().body(response);
        } catch (RestClientException ex) {
            return unavailable("order-service", ex);
        }
    }

    @GetMapping("/orders/{orderId}")
    ResponseEntity<?> getOrder(@PathVariable UUID orderId) {
        try {
            ResponseEntity<String> response = orderClient.get()
                    .uri("/orders/{orderId}", orderId)
                    .retrieve()
                    .toEntity(String.class);
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (RestClientException ex) {
            return unavailable("order-service", ex);
        }
    }

    @GetMapping("/exposures/{clientId}/{symbol}")
    ResponseEntity<?> getExposure(@PathVariable String clientId, @PathVariable String symbol) {
        try {
            ExposureSummary response = historyClient.get()
                    .uri("/exposures/{clientId}/{symbol}", clientId, symbol)
                    .retrieve()
                    .body(ExposureSummary.class);
            return ResponseEntity.ok(response);
        } catch (RestClientException ex) {
            return unavailable("history-service", ex);
        }
    }

    @GetMapping("/exposures/{clientId}")
    ResponseEntity<?> recentExposureEvents(@PathVariable String clientId) {
        try {
            ResponseEntity<String> response = historyClient.get()
                    .uri("/exposures/{clientId}", clientId)
                    .retrieve()
                    .toEntity(String.class);
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (RestClientException ex) {
            return unavailable("history-service", ex);
        }
    }

    private ResponseEntity<Map<String, String>> unavailable(String service, RestClientException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "downstream unavailable",
                        "service", service,
                        "exception", ex.getClass().getSimpleName()
                ));
    }
}

