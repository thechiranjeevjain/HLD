package com.example.risk.order.service;

import com.example.risk.common.OrderRequest;
import com.example.risk.common.OrderResponse;
import com.example.risk.common.OrderSide;
import com.example.risk.common.OrderStatus;
import com.example.risk.common.RiskCheckResponse;
import com.example.risk.order.client.RiskClient;
import com.example.risk.order.domain.OrderEntity;
import com.example.risk.order.messaging.OrderEventPublisher;
import com.example.risk.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderApplicationServiceTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final RiskClient riskClient = mock(RiskClient.class);
    private final OrderEventPublisher publisher = mock(OrderEventPublisher.class);
    private final OrderApplicationService service = new OrderApplicationService(repository, riskClient, publisher);

    @Test
    void storesAcceptedOrderAndPublishesEvent() {
        when(riskClient.check(any())).thenReturn(RiskCheckResponse.accept("ok"));
        when(repository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = service.accept(new OrderRequest(
                "CLIENT-A",
                "AAPL",
                OrderSide.BUY,
                10,
                new BigDecimal("100.00")
        ));

        assertThat(response.status()).isEqualTo(OrderStatus.ACCEPTED);
        verify(repository).save(any(OrderEntity.class));
        verify(publisher).publish(any());
    }
}

