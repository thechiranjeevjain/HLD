package com.example.capstone.ecommerce.payment;

import com.example.capstone.ecommerce.error.DomainException;
import com.example.capstone.ecommerce.error.NotFoundException;
import com.example.capstone.ecommerce.inventory.InventoryItem;
import com.example.capstone.ecommerce.inventory.InventoryRepository;
import com.example.capstone.ecommerce.order.CustomerOrder;
import com.example.capstone.ecommerce.order.OrderEventPublisher;
import com.example.capstone.ecommerce.order.OrderRepository;
import com.example.capstone.ecommerce.order.OrderStatus;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;
    private final OrderEventPublisher eventPublisher;

    public PaymentService(
            OrderRepository orderRepository,
            InventoryRepository inventoryRepository,
            PaymentRepository paymentRepository,
            OrderEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    public PaymentResponse capture(UUID orderId, PaymentRequest request) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new DomainException("Only RESERVED orders can be paid");
        }

        boolean declined = request.paymentToken().toLowerCase(Locale.ROOT).startsWith("fail");
        if (declined) {
            order.getLines().forEach(line -> inventory(line.getSku()).releaseReserved(line.getQuantity()));
            order.markPaymentFailed();
            PaymentRecord record = paymentRepository.save(new PaymentRecord(
                    orderId,
                    order.getTotalAmount(),
                    order.getCurrency(),
                    PaymentStatus.DECLINED,
                    "sim-" + UUID.randomUUID(),
                    "Simulated processor decline"
            ));
            eventPublisher.publish(orderId, "PAYMENT_DECLINED", order.getStatus());
            return PaymentResponse.from(record);
        }

        order.getLines().forEach(line -> inventory(line.getSku()).captureReserved(line.getQuantity()));
        order.markPaid();
        PaymentRecord record = paymentRepository.save(new PaymentRecord(
                orderId,
                order.getTotalAmount(),
                order.getCurrency(),
                PaymentStatus.CAPTURED,
                "sim-" + UUID.randomUUID(),
                null
        ));
        eventPublisher.publish(orderId, "PAYMENT_CAPTURED", order.getStatus());
        return PaymentResponse.from(record);
    }

    private InventoryItem inventory(String sku) {
        return inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("Inventory item not found: " + sku));
    }
}
