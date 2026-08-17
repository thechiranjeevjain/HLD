package com.example.capstone.shortener.rate;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisRateLimiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisRateLimiter.class);
    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean allow(String bucket, int limit, Duration window) {
        try {
            String key = "rate:" + bucket;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, window);
            }
            return count != null && count <= limit;
        } catch (RuntimeException exception) {
            LOGGER.warn("Redis rate limiter unavailable; allowing request", exception);
            return true;
        }
    }
}
