package com.example.risk.notification.service;

import com.example.risk.common.OrderEvent;
import com.example.risk.common.OrderSide;
import com.example.risk.common.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceTest {
    @Test
    void publishesRecentAlert() {
        NotificationService service = new NotificationService();

        service.publish(new OrderEvent(
                UUID.randomUUID(), "CLIENT-A", "MSFT", OrderSide.BUY, 10, new BigDecimal("100.00"),
                new BigDecimal("1000.00"), OrderStatus.REJECTED, "over limit", Instant.now()));

        assertThat(service.recent()).hasSize(1);
        assertThat(service.recent().get(0).message()).contains("over limit");
    }
}
