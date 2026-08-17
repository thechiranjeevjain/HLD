package com.example.risk.order.api;

import com.example.risk.common.OrderRequest;
import com.example.risk.common.OrderResponse;
import com.example.risk.order.domain.OrderEntity;
import com.example.risk.order.service.OrderApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderApplicationService orderService;

    public OrderController(OrderApplicationService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.accepted().body(orderService.accept(request));
    }

    @GetMapping("/{orderId}")
    ResponseEntity<OrderEntity> get(@PathVariable UUID orderId) {
        return orderService.find(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

