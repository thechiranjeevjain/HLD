package com.example.risk.risk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@ConfigurationProperties(prefix = "risk.limits")
public class RiskLimitProperties {
    private BigDecimal maxOrderNotional = new BigDecimal("500000");
    private BigDecimal maxDailyExposure = new BigDecimal("2500000");
    private long maxOrderQuantity = 10000;
    private Set<String> allowedSymbols = new LinkedHashSet<>(Set.of("AAPL", "AMZN", "GOOG", "MSFT", "NVDA", "TSLA"));

    public BigDecimal getMaxOrderNotional() {
        return maxOrderNotional;
    }

    public void setMaxOrderNotional(BigDecimal maxOrderNotional) {
        this.maxOrderNotional = maxOrderNotional;
    }

    public BigDecimal getMaxDailyExposure() {
        return maxDailyExposure;
    }

    public void setMaxDailyExposure(BigDecimal maxDailyExposure) {
        this.maxDailyExposure = maxDailyExposure;
    }

    public long getMaxOrderQuantity() {
        return maxOrderQuantity;
    }

    public void setMaxOrderQuantity(long maxOrderQuantity) {
        this.maxOrderQuantity = maxOrderQuantity;
    }

    public Set<String> getAllowedSymbols() {
        return allowedSymbols;
    }

    public void setAllowedSymbols(Set<String> allowedSymbols) {
        this.allowedSymbols = allowedSymbols == null ? Set.of() : normalize(allowedSymbols);
    }

    public boolean symbolAllowed(String symbol) {
        return allowedSymbols.isEmpty() || allowedSymbols.contains(symbol.toUpperCase());
    }

    private Set<String> normalize(Set<String> symbols) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String symbol : symbols) {
            normalized.add(symbol.toUpperCase());
        }
        return normalized;
    }
}
