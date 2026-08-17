package com.example.risk.order.service;

import com.example.risk.common.ExposureSummary;
import com.example.risk.common.OrderRequest;
import com.example.risk.common.OrderResponse;
import com.example.risk.common.OrderSide;
import com.example.risk.common.OrderStatus;
import com.example.risk.common.RiskCheckResponse;
import com.example.risk.order.client.HistoryServiceClient;
import com.example.risk.order.client.NotificationServiceClient;
import com.example.risk.order.client.RiskServiceClient;
import com.example.risk.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderProcessingServiceTest {
    @Test
    void acceptedOrderIsRecordedInHistory() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        RiskServiceClient riskClient = mock(RiskServiceClient.class);
        HistoryServiceClient historyClient = mock(HistoryServiceClient.class);
        NotificationServiceClient notificationClient = mock(NotificationServiceClient.class);
        OrderProcessingService service = new OrderProcessingService(orderRepository, riskClient, historyClient,
                notificationClient, new BigDecimal("1000000"));

        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(riskClient.check(any())).thenReturn(RiskCheckResponse.accept("Within limits"));
        when(historyClient.record(any())).thenReturn(ExposureSummary.zero("CLIENT-A", "MSFT"));

        OrderResponse response = service.submit(new OrderRequest(
                "CLIENT-A", "MSFT", OrderSide.BUY, 100, new BigDecimal("410.25")));

        assertThat(response.status()).isEqualTo(OrderStatus.ACCEPTED);
        verify(historyClient).record(any());
    }

    @Test
    void riskOutageRejectsFailClosed() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        RiskServiceClient riskClient = mock(RiskServiceClient.class);
        HistoryServiceClient historyClient = mock(HistoryServiceClient.class);
        NotificationServiceClient notificationClient = mock(NotificationServiceClient.class);
        OrderProcessingService service = new OrderProcessingService(orderRepository, riskClient, historyClient,
                notificationClient, new BigDecimal("1000000"));

        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(riskClient.check(any())).thenThrow(new IllegalStateException("boom"));
        when(historyClient.record(any())).thenReturn(ExposureSummary.zero("CLIENT-A", "MSFT"));

        OrderResponse response = service.submit(new OrderRequest(
                "CLIENT-A", "MSFT", OrderSide.BUY, 100, new BigDecimal("410.25")));

        assertThat(response.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(response.reason()).contains("fail-closed");
        verify(notificationClient).publishAlert(any());
    }
}
