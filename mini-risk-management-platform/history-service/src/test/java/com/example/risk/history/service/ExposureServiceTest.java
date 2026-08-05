package com.example.risk.history.service;

import com.example.risk.common.OrderEvent;
import com.example.risk.common.OrderSide;
import com.example.risk.common.OrderStatus;
import com.example.risk.history.domain.ExposureEvent;
import com.example.risk.history.repository.ExposureEventRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExposureServiceTest {
    private final ExposureEventRepository repository = mock(ExposureEventRepository.class);
    private final ExposureService service = new ExposureService(repository);

    @Test
    void ignoresRejectedOrders() {
        OrderEvent event = event(OrderStatus.REJECTED);

        service.record(event);

        verify(repository, never()).save(any(ExposureEvent.class));
    }

    @Test
    void storesAcceptedOrderOnce() {
        OrderEvent event = event(OrderStatus.ACCEPTED);
        when(repository.existsByOrderId(event.orderId())).thenReturn(false);

        service.record(event);

        verify(repository).save(any(ExposureEvent.class));
    }

    private OrderEvent event(OrderStatus status) {
        return new OrderEvent(
                UUID.randomUUID(),
                "CLIENT-A",
                "AAPL",
                OrderSide.BUY,
                10,
                new BigDecimal("100.00"),
                new BigDecimal("1000.00"),
                status,
                "test",
                Instant.now()
        );
    }
}

