package com.example.risk.history.service;

import com.example.risk.common.ExposureSummary;
import com.example.risk.common.OrderEvent;
import com.example.risk.common.OrderStatus;
import com.example.risk.history.domain.ExposureEvent;
import com.example.risk.history.repository.ExposureAggregate;
import com.example.risk.history.repository.ExposureEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ExposureService {
    private final ExposureEventRepository repository;

    public ExposureService(ExposureEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(OrderEvent event) {
        if (event.status() != OrderStatus.ACCEPTED || repository.existsByOrderId(event.orderId())) {
            return;
        }
        repository.save(new ExposureEvent(
                event.orderId(),
                event.clientId(),
                event.symbol().toUpperCase(),
                event.side(),
                event.quantity(),
                event.price(),
                event.notional(),
                event.status(),
                event.occurredAt()
        ));
    }

    @Transactional(readOnly = true)
    public ExposureSummary summary(String clientId, String symbol) {
        String normalizedSymbol = symbol.toUpperCase();
        ExposureAggregate aggregate = repository.aggregate(clientId, normalizedSymbol);
        return new ExposureSummary(
                clientId,
                normalizedSymbol,
                aggregate.getNetQuantity() == null ? 0 : aggregate.getNetQuantity(),
                aggregate.getGrossNotional() == null ? BigDecimal.ZERO : aggregate.getGrossNotional(),
                aggregate.getDailyExposure() == null ? BigDecimal.ZERO : aggregate.getDailyExposure()
        );
    }

    @Transactional(readOnly = true)
    public List<ExposureEvent> recent(String clientId) {
        return repository.findTop50ByClientIdOrderByOccurredAtDesc(clientId);
    }
}

