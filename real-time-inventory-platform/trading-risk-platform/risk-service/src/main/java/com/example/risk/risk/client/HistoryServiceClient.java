package com.example.risk.risk.client;

import com.example.risk.common.ExposureSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HistoryServiceClient {
    private final RestClient restClient;

    public HistoryServiceClient(@Value("${services.history.base-url}") String baseUrl, RestClient.Builder builder) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ExposureSummary exposureFor(String clientId, String symbol) {
        return restClient.get()
                .uri("/exposures/{clientId}/{symbol}", clientId, symbol)
                .retrieve()
                .body(ExposureSummary.class);
    }
}
