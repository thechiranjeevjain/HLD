package com.example.risk.risk.service;

import com.example.risk.risk.repository.RiskLimitRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RiskLimitLookup {
    private static final Logger log = LoggerFactory.getLogger(RiskLimitLookup.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final RiskLimitRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RiskLimitLookup(RiskLimitRepository repository, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<RiskLimitSnapshot> find(String clientId, String symbol) {
        String normalizedSymbol = symbol.toUpperCase();
        String key = "risk-limit:%s:%s".formatted(clientId, normalizedSymbol);
        Optional<RiskLimitSnapshot> cached = readCache(key);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<RiskLimitSnapshot> loaded = repository.findByClientIdAndSymbol(clientId, normalizedSymbol)
                .map(RiskLimitSnapshot::from);
        loaded.ifPresent(limit -> writeCache(key, limit));
        return loaded;
    }

    private Optional<RiskLimitSnapshot> readCache(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, RiskLimitSnapshot.class));
        } catch (RedisConnectionFailureException | JsonProcessingException ex) {
            log.warn("risk-limit cache read skipped for key={} reason={}", key, ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private void writeCache(String key, RiskLimitSnapshot value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), CACHE_TTL);
        } catch (RedisConnectionFailureException | JsonProcessingException ex) {
            log.warn("risk-limit cache write skipped for key={} reason={}", key, ex.getClass().getSimpleName());
        }
    }
}

