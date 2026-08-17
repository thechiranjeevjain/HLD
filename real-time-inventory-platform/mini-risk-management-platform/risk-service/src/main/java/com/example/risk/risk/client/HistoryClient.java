package com.example.risk.risk.client;

import com.example.risk.common.ExposureSummary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
public class HistoryClient {
    private final RestClient restClient;

    public HistoryClient(RestClient historyRestClient) {
        this.restClient = historyRestClient;
    }

    public Optional<ExposureSummary> exposure(String clientId, String symbol) {
        try {
            ExposureSummary summary = restClient.get()
                    .uri("/exposures/{clientId}/{symbol}", clientId, symbol.toUpperCase())
                    .retrieve()
                    .body(ExposureSummary.class);
            return Optional.ofNullable(summary);
        } catch (RestClientException ex) {
            return Optional.empty();
        }
    }
}

