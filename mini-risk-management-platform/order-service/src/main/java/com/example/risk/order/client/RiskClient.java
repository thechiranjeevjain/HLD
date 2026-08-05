package com.example.risk.order.client;

import com.example.risk.common.RiskCheckRequest;
import com.example.risk.common.RiskCheckResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RiskClient {
    private final RestClient restClient;

    public RiskClient(RestClient riskRestClient) {
        this.restClient = riskRestClient;
    }

    public RiskCheckResponse check(RiskCheckRequest request) {
        try {
            RiskCheckResponse response = restClient.post()
                    .uri("/risk/check")
                    .body(request)
                    .retrieve()
                    .body(RiskCheckResponse.class);
            return response == null ? RiskCheckResponse.reject("risk-service returned empty response") : response;
        } catch (RestClientException ex) {
            return RiskCheckResponse.reject("risk-service unavailable: " + ex.getClass().getSimpleName());
        }
    }
}
