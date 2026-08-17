package com.example.risk.risk.service;

import com.example.risk.common.ExposureSummary;
import com.example.risk.common.OrderSide;
import com.example.risk.common.RiskCheckRequest;
import com.example.risk.common.RiskCheckResponse;
import com.example.risk.common.RiskDecision;
import com.example.risk.risk.client.HistoryClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskEvaluationServiceTest {
    private final RiskLimitLookup limitLookup = mock(RiskLimitLookup.class);
    private final HistoryClient historyClient = mock(HistoryClient.class);
    private final RiskEvaluationService service = new RiskEvaluationService(limitLookup, historyClient);

    @Test
    void acceptsOrderInsideAllLimits() {
        when(limitLookup.find("CLIENT-A", "AAPL")).thenReturn(Optional.of(limit()));
        when(historyClient.exposure("CLIENT-A", "AAPL")).thenReturn(Optional.of(
                new ExposureSummary("CLIENT-A", "AAPL", 100, new BigDecimal("10000.00"), new BigDecimal("10000.00"))
        ));

        RiskCheckResponse response = service.evaluate(request(250, "100.00"));

        assertThat(response.decision()).isEqualTo(RiskDecision.ACCEPT);
    }

    @Test
    void rejectsOrderQuantityAboveLimit() {
        when(limitLookup.find("CLIENT-A", "AAPL")).thenReturn(Optional.of(limit()));

        RiskCheckResponse response = service.evaluate(request(6000, "100.00"));

        assertThat(response.decision()).isEqualTo(RiskDecision.REJECT);
        assertThat(response.reason()).contains("exceeds max order quantity");
    }

    @Test
    void rejectsWhenHistoryUnavailable() {
        when(limitLookup.find("CLIENT-A", "AAPL")).thenReturn(Optional.of(limit()));
        when(historyClient.exposure("CLIENT-A", "AAPL")).thenReturn(Optional.empty());

        RiskCheckResponse response = service.evaluate(request(100, "100.00"));

        assertThat(response.decision()).isEqualTo(RiskDecision.REJECT);
        assertThat(response.reason()).contains("fail-closed");
    }

    private RiskLimitSnapshot limit() {
        return new RiskLimitSnapshot("CLIENT-A", "AAPL", 5000, 1000, new BigDecimal("100000.00"));
    }

    private RiskCheckRequest request(long quantity, String price) {
        return new RiskCheckRequest(UUID.randomUUID(), "CLIENT-A", "AAPL", OrderSide.BUY, quantity, new BigDecimal(price));
    }
}

