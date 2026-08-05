package com.example.risk.order.client;

import com.example.risk.common.ExposureSummary;
import com.example.risk.common.OrderEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HistoryServiceClient {
    private final RestClient restClient;

    public HistoryServiceClient(@Value("${services.history.base-url}") String baseUrl, RestClient.Builder builder) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ExposureSummary record(OrderEvent event) {
        return restClient.post()
                .uri("/events")
                .body(event)
                .retrieve()
                .body(ExposureSummary.class);
    }
}
