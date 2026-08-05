package com.example.risk.order.client;

import com.example.risk.common.OrderEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificationServiceClient {
    private final RestClient restClient;

    public NotificationServiceClient(@Value("${services.notification.base-url}") String baseUrl,
                                     RestClient.Builder builder) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public void publishAlert(OrderEvent event) {
        restClient.post()
                .uri("/alerts")
                .body(event)
                .retrieve()
                .toBodilessEntity();
    }
}
