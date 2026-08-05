package com.example.capstone.ecommerce.order;

import com.example.capstone.ecommerce.payment.PaymentRequest;
import com.example.capstone.ecommerce.payment.PaymentResponse;
import com.example.capstone.ecommerce.payment.PaymentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @PostMapping
    public OrderResponse placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(request);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return orderService.get(id);
    }

    @PostMapping("/{id}/payments")
    public PaymentResponse capturePayment(@PathVariable UUID id, @Valid @RequestBody PaymentRequest request) {
        return paymentService.capture(id, request);
    }
}
