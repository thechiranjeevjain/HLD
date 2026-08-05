package com.example.risk.pretrade;

import com.example.risk.pretrade.Models.KillSwitchRequest;
import com.example.risk.pretrade.Models.OrderRequest;
import com.example.risk.pretrade.Models.OrderStatus;
import com.example.risk.pretrade.Models.Side;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PreTradeRiskEngineTest {
    @Test
    void acceptsAndReservesExposureInMemory() {
        PreTradeRiskEngine engine = new PreTradeRiskEngine();

        var decision = engine.submit(new OrderRequest("OK-1", "ACCT-DEMO", "MSFT", Side.BUY, 100,
                new BigDecimal("410.25"), false));

        assertThat(decision.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(engine.state().accounts().get("ACCT-DEMO").reservedNotional()).isEqualByComparingTo("41025.0000");
    }

    @Test
    void killSwitchRejectsBeforeReservation() {
        PreTradeRiskEngine engine = new PreTradeRiskEngine();
        engine.setKillSwitch(new KillSwitchRequest("ACCOUNT", "ACCT-DEMO", true, "test"));

        var decision = engine.submit(new OrderRequest("KILL-1", "ACCT-DEMO", "MSFT", Side.BUY, 100,
                new BigDecimal("410.25"), false));

        assertThat(decision.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(decision.reason()).contains("Active kill switch");
        assertThat(engine.state().accounts().get("ACCT-DEMO").reservedNotional()).isEqualByComparingTo("0.0000");
    }

    @Test
    void raceDemoAllowsOnlyOneCompetingReservation() {
        PreTradeRiskEngine engine = new PreTradeRiskEngine();

        var scenario = engine.runScenario("race");

        assertThat(scenario.decisions()).hasSize(2);
        assertThat(scenario.decisions()).filteredOn(decision -> decision.status() == OrderStatus.ACCEPTED).hasSize(1);
        assertThat(scenario.decisions()).filteredOn(decision -> decision.status() == OrderStatus.REJECTED).hasSize(1);
    }

    @Test
    void fillAndMarketTickUpdateUnrealizedPnl() {
        PreTradeRiskEngine engine = new PreTradeRiskEngine();

        engine.runScenario("pnl");

        var position = engine.state().positions().stream()
                .filter(snapshot -> snapshot.symbol().equals("MSFT"))
                .findFirst()
                .orElseThrow();
        assertThat(position.netQuantity()).isEqualTo(100);
        assertThat(position.unrealizedPnl()).isEqualByComparingTo("850.0000");
    }
}
