package com.example.risk.history.service;

import com.example.risk.common.OrderEvent;
import com.example.risk.common.OrderSide;
import com.example.risk.common.OrderStatus;
import com.example.risk.history.repository.ExposureRepository;
import com.example.risk.history.repository.OrderEventRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HistoryServiceTest {
    @Test
    void acceptedBuyOrderIncreasesExposure() {
        OrderEventRepository eventRepository = mock(OrderEventRepository.class);
        ExposureRepository exposureRepository = mock(ExposureRepository.class);
        HistoryService service = new HistoryService(eventRepository, exposureRepository);
        when(eventRepository.existsById(any())).thenReturn(false);
        when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(exposureRepository.findWithLockByClientIdAndSymbol("CLIENT-A", "MSFT")).thenReturn(Optional.empty());
        when(exposureRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var summary = service.record(new OrderEvent(
                UUID.randomUUID(), "CLIENT-A", "MSFT", OrderSide.BUY, 10, new BigDecimal("100.00"),
                new BigDecimal("1000.00"), OrderStatus.ACCEPTED, "ok", Instant.now()));

        assertThat(summary.netQuantity()).isEqualTo(10);
        assertThat(summary.dailyExposure()).isEqualByComparingTo("1000.00");
    }
}
