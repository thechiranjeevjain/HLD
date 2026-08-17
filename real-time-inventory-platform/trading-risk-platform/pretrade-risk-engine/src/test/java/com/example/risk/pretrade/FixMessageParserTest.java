package com.example.risk.pretrade;

import com.example.risk.pretrade.Models.Side;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixMessageParserTest {
    private final FixMessageParser parser = new FixMessageParser();

    @Test
    void parsesNewOrderSingleWithPipeDelimiter() {
        var order = parser.parse("8=FIX.4.4|35=D|11=ABC-1|1=ACCT-DEMO|55=MSFT|54=1|38=100|44=410.25|10=000|");

        assertThat(order.clOrdId()).isEqualTo("ABC-1");
        assertThat(order.account()).isEqualTo("ACCT-DEMO");
        assertThat(order.symbol()).isEqualTo("MSFT");
        assertThat(order.side()).isEqualTo(Side.BUY);
        assertThat(order.quantity()).isEqualTo(100);
        assertThat(order.price()).isEqualByComparingTo("410.2500");
    }

    @Test
    void rejectsUnsupportedMessageType() {
        assertThatThrownBy(() -> parser.parse("8=FIX.4.4|35=F|11=ABC-1|1=ACCT-DEMO|55=MSFT|54=1|38=100|44=410.25|"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected 35=D");
    }
}
