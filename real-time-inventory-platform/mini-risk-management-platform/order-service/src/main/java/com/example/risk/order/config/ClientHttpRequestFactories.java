package com.example.risk.order.config;

import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

final class ClientHttpRequestFactories {
    private ClientHttpRequestFactories() {
    }

    static JdkClientHttpRequestFactory withTimeouts(Duration connectTimeout, Duration readTimeout) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}

