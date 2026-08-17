package com.example.risk.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {
    @Bean
    RestClient riskRestClient(
            RestClient.Builder builder,
            @Value("${services.risk-service.base-url}") String riskServiceBaseUrl
    ) {
        return builder
                .baseUrl(riskServiceBaseUrl)
                .requestFactory(ClientHttpRequestFactories.withTimeouts(Duration.ofMillis(750), Duration.ofSeconds(2)))
                .build();
    }
}

