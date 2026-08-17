package com.example.risk.order.service;

import com.example.risk.common.OrderEvent;
import com.example.risk.common.OrderRequest;
import com.example.risk.common.OrderResponse;
import com.example.risk.common.OrderStatus;
import com.example.risk.common.RiskCheckRequest;
import com.example.risk.common.RiskCheckResponse;
import com.example.risk.common.RiskDecision;
import com.example.risk.order.client.RiskClient;
import com.example.risk.order.domain.OrderEntity;
import com.example.risk.order.messaging.OrderEventPublisher;
import com.example.risk.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderApplicationService {
    private final OrderRepository orderRepository;
    private final RiskClient riskClient;
    private final OrderEventPublisher publisher;

    public OrderApplicationService(OrderRepository orderRepository, RiskClient riskClient, OrderEventPublisher publisher) {
        this.orderRepository = orderRepository;
        this.riskClient = riskClient;
        this.publisher = publisher;
    }

    @Transactional
    public OrderResponse accept(OrderRequest request) {
        UUID orderId = UUID.randomUUID();
        RiskCheckResponse risk = riskClient.check(new RiskCheckRequest(
                orderId,
                request.clientId(),
                request.symbol(),
                request.side(),
                request.quantity(),
                request.price()
        ));

        OrderStatus status = risk.decision() == RiskDecision.ACCEPT ? OrderStatus.ACCEPTED : OrderStatus.REJECTED;
        Instant now = Instant.now();
        OrderEntity order = new OrderEntity(
                orderId,
                request.clientId(),
                request.symbol(),
                request.side(),
                request.quantity(),
                request.price(),
                request.notional(),
                status,
                risk.reason(),
                now
        );

        orderRepository.save(order);
        publisher.publish(toEvent(order));
        return new OrderResponse(order.getId(), order.getStatus(), order.getReason());
    }

    @Transactional(readOnly = true)
    public Optional<OrderEntity> find(UUID id) {
        return orderRepository.findById(id);
    }

    private OrderEvent toEvent(OrderEntity order) {
        return new OrderEvent(
                order.getId(),
                order.getClientId(),
                order.getSymbol(),
                order.getSide(),
                order.getQuantity(),
                order.getPrice(),
                order.getNotional(),
                order.getStatus(),
                order.getReason(),
                order.getCreatedAt()
        );
    }
}

