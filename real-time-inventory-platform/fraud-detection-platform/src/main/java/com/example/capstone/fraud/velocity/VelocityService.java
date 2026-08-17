package com.example.capstone.fraud.velocity;

import com.example.capstone.fraud.transaction.TransactionEvent;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class VelocityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VelocityService.class);
    private static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC);

    private final StringRedisTemplate redisTemplate;

    public VelocityService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long recordAndCount(TransactionEvent event) {
        String key = "velocity:" + event.userId() + ":" + MINUTE_FORMAT.format(event.occurredAt());
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(2));
            }
            return count == null ? 1 : count;
        } catch (RuntimeException exception) {
            LOGGER.warn("Redis velocity check unavailable; falling back to count=1", exception);
            return 1;
        }
    }
}
