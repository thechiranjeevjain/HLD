package com.example.capstone.ecommerce.order;

import com.example.capstone.ecommerce.error.DomainException;
import com.example.capstone.ecommerce.error.NotFoundException;
import com.example.capstone.ecommerce.inventory.InventoryItem;
import com.example.capstone.ecommerce.inventory.InventoryRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public OrderService(InventoryRepository inventoryRepository, OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    public OrderResponse placeOrder(PlaceOrderRequest request) {
        List<OrderLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        String currency = null;

        for (OrderItemRequest itemRequest : request.items()) {
            String sku = itemRequest.sku().trim().toUpperCase(Locale.ROOT);
            InventoryItem item = inventoryRepository.findBySku(sku)
                    .orElseThrow(() -> new NotFoundException("Inventory item not found: " + sku));
            if (currency == null) {
                currency = item.getCurrency();
            } else if (!currency.equals(item.getCurrency())) {
                throw new DomainException("Orders cannot mix currencies");
            }
            item.reserve(itemRequest.quantity());
            OrderLine line = new OrderLine(sku, item.getName(), itemRequest.quantity(), item.getPrice());
            lines.add(line);
            total = total.add(line.getLineTotal());
        }

        CustomerOrder order = new CustomerOrder(request.customerId().trim(), total, currency);
        lines.forEach(order::addLine);
        CustomerOrder saved = orderRepository.save(order);
        eventPublisher.publish(saved.getId(), "ORDER_CREATED", saved.getStatus());
        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }
}
