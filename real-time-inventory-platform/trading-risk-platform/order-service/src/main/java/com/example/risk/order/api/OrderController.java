package com.example.risk.order.api;

import com.example.risk.common.OrderRequest;
import com.example.risk.common.OrderResponse;
import com.example.risk.order.domain.OrderEntity;
import com.example.risk.order.repository.OrderRepository;
import com.example.risk.order.service.OrderProcessingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderProcessingService orderProcessingService;
    private final OrderRepository orderRepository;

    public OrderController(OrderProcessingService orderProcessingService, OrderRepository orderRepository) {
        this.orderProcessingService = orderProcessingService;
        this.orderRepository = orderRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse submit(@Valid @RequestBody OrderRequest request) {
        return orderProcessingService.submit(request);
    }

    @GetMapping("/{orderId}")
    public OrderEntity get(@PathVariable UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    @GetMapping
    public List<OrderEntity> list() {
        return orderRepository.findTop50ByOrderByCreatedAtDesc();
    }
}
