package com.example.risk.risk.service;

import com.example.risk.common.ExposureSummary;
import com.example.risk.common.OrderSide;
import com.example.risk.common.RiskCheckRequest;
import com.example.risk.common.RiskDecision;
import com.example.risk.risk.client.HistoryServiceClient;
import com.example.risk.risk.config.RiskLimitProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskPolicyServiceTest {
    @Test
    void acceptsOrderInsideConfiguredLimits() {
        HistoryServiceClient historyClient = mock(HistoryServiceClient.class);
        RiskLimitProperties limits = new RiskLimitProperties();
        limits.setAllowedSymbols(Set.of("MSFT"));
        RiskPolicyService service = new RiskPolicyService(historyClient, limits);
        when(historyClient.exposureFor("CLIENT-A", "MSFT"))
                .thenReturn(ExposureSummary.zero("CLIENT-A", "MSFT"));

        var response = service.check(new RiskCheckRequest(
                UUID.randomUUID(), "CLIENT-A", "MSFT", OrderSide.BUY, 100, new BigDecimal("100.00")));

        assertThat(response.decision()).isEqualTo(RiskDecision.ACCEPT);
    }

    @Test
    void rejectsProjectedDailyExposureOverLimit() {
        HistoryServiceClient historyClient = mock(HistoryServiceClient.class);
        RiskLimitProperties limits = new RiskLimitProperties();
        limits.setAllowedSymbols(Set.of("MSFT"));
        limits.setMaxDailyExposure(new BigDecimal("1000.00"));
        RiskPolicyService service = new RiskPolicyService(historyClient, limits);
        when(historyClient.exposureFor("CLIENT-A", "MSFT"))
                .thenReturn(new ExposureSummary("CLIENT-A", "MSFT", 0, BigDecimal.ZERO, new BigDecimal("900.00")));

        var response = service.check(new RiskCheckRequest(
                UUID.randomUUID(), "CLIENT-A", "MSFT", OrderSide.BUY, 2, new BigDecimal("100.00")));

        assertThat(response.decision()).isEqualTo(RiskDecision.REJECT);
        assertThat(response.reason()).contains("Projected daily exposure");
    }
}
