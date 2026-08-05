package com.example.risk.risk.service;

import com.example.risk.common.ExposureSummary;
import com.example.risk.common.RiskCheckRequest;
import com.example.risk.common.RiskCheckResponse;
import com.example.risk.risk.client.HistoryServiceClient;
import com.example.risk.risk.config.RiskLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RiskPolicyService {
    private static final Logger log = LoggerFactory.getLogger(RiskPolicyService.class);

    private final HistoryServiceClient historyServiceClient;
    private final RiskLimitProperties limits;

    public RiskPolicyService(HistoryServiceClient historyServiceClient, RiskLimitProperties limits) {
        this.historyServiceClient = historyServiceClient;
        this.limits = limits;
    }

    public RiskCheckResponse check(RiskCheckRequest request) {
        String symbol = request.symbol().toUpperCase();
        BigDecimal notional = request.notional();

        if (!limits.symbolAllowed(symbol)) {
            return RiskCheckResponse.reject("Symbol is not approved for this risk lab: " + symbol);
        }

        if (request.quantity() > limits.getMaxOrderQuantity()) {
            return RiskCheckResponse.reject("Quantity exceeds max order quantity " + limits.getMaxOrderQuantity());
        }

        if (notional.compareTo(limits.getMaxOrderNotional()) > 0) {
            return RiskCheckResponse.reject("Notional exceeds max order notional " + limits.getMaxOrderNotional());
        }

        ExposureSummary exposure = currentExposure(request.clientId(), symbol);
        BigDecimal projectedDailyExposure = exposure.dailyExposure().add(notional);
        if (projectedDailyExposure.compareTo(limits.getMaxDailyExposure()) > 0) {
            return RiskCheckResponse.reject("Projected daily exposure " + projectedDailyExposure
                    + " exceeds limit " + limits.getMaxDailyExposure());
        }

        return RiskCheckResponse.accept("Within configured risk limits");
    }

    private ExposureSummary currentExposure(String clientId, String symbol) {
        try {
            ExposureSummary exposure = historyServiceClient.exposureFor(clientId, symbol);
            return exposure == null ? ExposureSummary.zero(clientId, symbol) : exposure;
        } catch (RuntimeException ex) {
            log.warn("History service unavailable while checking exposure for {}/{}", clientId, symbol, ex);
            throw ex;
        }
    }
}
