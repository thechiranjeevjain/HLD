package com.example.risk.order.client;

import com.example.risk.common.RiskCheckRequest;
import com.example.risk.common.RiskCheckResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RiskServiceClient {
    private final RestClient restClient;

    public RiskServiceClient(@Value("${services.risk.base-url}") String baseUrl, RestClient.Builder builder) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public RiskCheckResponse check(RiskCheckRequest request) {
        return restClient.post()
                .uri("/risk/check")
                .body(request)
                .retrieve()
                .body(RiskCheckResponse.class);
    }
}
