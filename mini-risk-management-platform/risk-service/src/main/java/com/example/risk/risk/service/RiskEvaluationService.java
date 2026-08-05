package com.example.risk.risk.service;

import com.example.risk.common.ExposureSummary;
import com.example.risk.common.OrderSide;
import com.example.risk.common.RiskCheckRequest;
import com.example.risk.common.RiskCheckResponse;
import com.example.risk.risk.client.HistoryClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RiskEvaluationService {
    private final RiskLimitLookup limitLookup;
    private final HistoryClient historyClient;

    public RiskEvaluationService(RiskLimitLookup limitLookup, HistoryClient historyClient) {
        this.limitLookup = limitLookup;
        this.historyClient = historyClient;
    }

    public RiskCheckResponse evaluate(RiskCheckRequest request) {
        String symbol = request.symbol().toUpperCase();
        RiskLimitSnapshot limit = limitLookup.find(request.clientId(), symbol)
                .orElse(null);
        if (limit == null) {
            return RiskCheckResponse.reject("no risk limit configured for client=%s symbol=%s".formatted(request.clientId(), symbol));
        }

        if (request.quantity() > limit.maxOrderQuantity()) {
            return RiskCheckResponse.reject("order quantity %d exceeds max order quantity %d".formatted(
                    request.quantity(), limit.maxOrderQuantity()));
        }

        ExposureSummary exposure = historyClient.exposure(request.clientId(), symbol)
                .orElse(null);
        if (exposure == null) {
            return RiskCheckResponse.reject("history-service unavailable; fail-closed risk policy");
        }

        long signedQuantity = request.side() == OrderSide.BUY ? request.quantity() : -request.quantity();
        long projectedPosition = exposure.netQuantity() + signedQuantity;
        if (Math.abs(projectedPosition) > limit.maxPositionQuantity()) {
            return RiskCheckResponse.reject("projected position %d exceeds max position quantity %d".formatted(
                    projectedPosition, limit.maxPositionQuantity()));
        }

        BigDecimal projectedDailyExposure = exposure.dailyExposure().add(request.notional());
        if (projectedDailyExposure.compareTo(limit.maxDailyExposure()) > 0) {
            return RiskCheckResponse.reject("projected daily exposure %s exceeds max daily exposure %s".formatted(
                    projectedDailyExposure, limit.maxDailyExposure()));
        }

        return RiskCheckResponse.accept("accepted by quantity, position, and daily exposure rules");
    }
}

