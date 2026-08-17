package com.example.risk.order.service;

import com.example.risk.common.OrderEvent;
import com.example.risk.common.OrderRequest;
import com.example.risk.common.OrderResponse;
import com.example.risk.common.OrderStatus;
import com.example.risk.common.RiskCheckRequest;
import com.example.risk.common.RiskCheckResponse;
import com.example.risk.common.RiskDecision;
import com.example.risk.order.client.HistoryServiceClient;
import com.example.risk.order.client.NotificationServiceClient;
import com.example.risk.order.client.RiskServiceClient;
import com.example.risk.order.domain.OrderEntity;
import com.example.risk.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class OrderProcessingService {
    private static final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);

    private final OrderRepository orderRepository;
    private final RiskServiceClient riskServiceClient;
    private final HistoryServiceClient historyServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final BigDecimal largeOrderAlertNotional;
    private final Clock clock;

    public OrderProcessingService(OrderRepository orderRepository,
                                  RiskServiceClient riskServiceClient,
                                  HistoryServiceClient historyServiceClient,
                                  NotificationServiceClient notificationServiceClient,
                                  @Value("${alerts.large-order-notional}") BigDecimal largeOrderAlertNotional) {
        this(orderRepository, riskServiceClient, historyServiceClient, notificationServiceClient,
                largeOrderAlertNotional, Clock.systemUTC());
    }

    OrderProcessingService(OrderRepository orderRepository,
                           RiskServiceClient riskServiceClient,
                           HistoryServiceClient historyServiceClient,
                           NotificationServiceClient notificationServiceClient,
                           BigDecimal largeOrderAlertNotional,
                           Clock clock) {
        this.orderRepository = orderRepository;
        this.riskServiceClient = riskServiceClient;
        this.historyServiceClient = historyServiceClient;
        this.notificationServiceClient = notificationServiceClient;
        this.largeOrderAlertNotional = largeOrderAlertNotional;
        this.clock = clock;
    }

    @Transactional
    public OrderResponse submit(OrderRequest request) {
        UUID orderId = UUID.randomUUID();
        OrderEntity order = orderRepository.save(OrderEntity.pending(orderId, request, Instant.now(clock)));
        RiskCheckResponse riskDecision = riskDecisionFor(order, request);
        OrderStatus status = riskDecision.decision() == RiskDecision.ACCEPT ? OrderStatus.ACCEPTED : OrderStatus.REJECTED;

        order.mark(status, riskDecision.reason(), Instant.now(clock));
        orderRepository.save(order);

        OrderEvent event = order.toEvent();
        recordHistory(event);
        publishAlertIfNeeded(event);

        return new OrderResponse(order.getId(), order.getStatus(), order.getReason());
    }

    private RiskCheckResponse riskDecisionFor(OrderEntity order, OrderRequest request) {
        try {
            return riskServiceClient.check(new RiskCheckRequest(
                    order.getId(),
                    order.getClientId(),
                    order.getSymbol(),
                    order.getSide(),
                    order.getQuantity(),
                    request.price()));
        } catch (RuntimeException ex) {
            log.warn("Risk service unavailable for order {}; rejecting fail-closed", order.getId(), ex);
            return RiskCheckResponse.reject("Risk service unavailable; rejected fail-closed");
        }
    }

    private void recordHistory(OrderEvent event) {
        try {
            historyServiceClient.record(event);
        } catch (RuntimeException ex) {
            log.warn("History service did not record order event {}", event.orderId(), ex);
        }
    }

    private void publishAlertIfNeeded(OrderEvent event) {
        boolean rejected = event.status() == OrderStatus.REJECTED;
        boolean largeAcceptedOrder = event.status() == OrderStatus.ACCEPTED
                && event.notional().compareTo(largeOrderAlertNotional) >= 0;
        if (!rejected && !largeAcceptedOrder) {
            return;
        }

        try {
            notificationServiceClient.publishAlert(event);
        } catch (RuntimeException ex) {
            log.warn("Notification service did not accept alert for order {}", event.orderId(), ex);
        }
    }
}
