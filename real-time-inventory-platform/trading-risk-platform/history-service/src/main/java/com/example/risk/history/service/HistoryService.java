package com.example.risk.history.service;

import com.example.risk.common.ExposureSummary;
import com.example.risk.common.OrderEvent;
import com.example.risk.common.OrderStatus;
import com.example.risk.history.domain.ExposureEntity;
import com.example.risk.history.domain.OrderEventEntity;
import com.example.risk.history.repository.ExposureRepository;
import com.example.risk.history.repository.OrderEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class HistoryService {
    private final OrderEventRepository orderEventRepository;
    private final ExposureRepository exposureRepository;

    public HistoryService(OrderEventRepository orderEventRepository, ExposureRepository exposureRepository) {
        this.orderEventRepository = orderEventRepository;
        this.exposureRepository = exposureRepository;
    }

    @Transactional
    public ExposureSummary record(OrderEvent event) {
        String symbol = event.symbol().toUpperCase();
        if (orderEventRepository.existsById(event.orderId())) {
            return exposure(event.clientId(), symbol);
        }

        orderEventRepository.save(OrderEventEntity.from(event));
        if (event.status() != OrderStatus.ACCEPTED) {
            return exposure(event.clientId(), symbol);
        }

        ExposureEntity exposure = exposureRepository.findWithLockByClientIdAndSymbol(event.clientId(), symbol)
                .orElseGet(() -> new ExposureEntity(event.clientId(), symbol));
        exposure.apply(event);
        return exposureRepository.save(exposure).toSummary();
    }

    @Transactional(readOnly = true)
    public ExposureSummary exposure(String clientId, String symbol) {
        return exposureRepository.findByClientIdAndSymbol(clientId, symbol.toUpperCase())
                .map(ExposureEntity::toSummary)
                .orElseGet(() -> ExposureSummary.zero(clientId, symbol.toUpperCase()));
    }

    @Transactional(readOnly = true)
    public List<ExposureSummary> exposures() {
        return exposureRepository.findAll().stream()
                .map(ExposureEntity::toSummary)
                .sorted(Comparator.comparing(ExposureSummary::clientId).thenComparing(ExposureSummary::symbol))
                .toList();
    }
}
