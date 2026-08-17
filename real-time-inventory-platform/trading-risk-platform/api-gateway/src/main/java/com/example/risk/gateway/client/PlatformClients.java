package com.example.risk.gateway.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PlatformClients {
    private final RestClient order;
    private final RestClient history;
    private final RestClient notification;

    public PlatformClients(@Value("${services.order.base-url}") String orderBaseUrl,
                           @Value("${services.history.base-url}") String historyBaseUrl,
                           @Value("${services.notification.base-url}") String notificationBaseUrl,
                           RestClient.Builder builder) {
        this.order = builder.baseUrl(orderBaseUrl).build();
        this.history = builder.baseUrl(historyBaseUrl).build();
        this.notification = builder.baseUrl(notificationBaseUrl).build();
    }

    public RestClient order() {
        return order;
    }

    public RestClient history() {
        return history;
    }

    public RestClient notification() {
        return notification;
    }
}
