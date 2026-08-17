package com.example.risk.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {
    @Bean("orderRestClient")
    RestClient orderRestClient(RestClient.Builder builder, @Value("${services.order-service.base-url}") String baseUrl) {
        return client(builder, baseUrl);
    }

    @Bean("historyRestClient")
    RestClient historyRestClient(RestClient.Builder builder, @Value("${services.history-service.base-url}") String baseUrl) {
        return client(builder, baseUrl);
    }

    private RestClient client(RestClient.Builder builder, String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(750))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(3));
        return builder.baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}

