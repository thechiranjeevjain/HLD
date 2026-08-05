package com.example.risk.history.repository;

import java.math.BigDecimal;

public interface ExposureAggregate {
    Long getNetQuantity();

    BigDecimal getGrossNotional();

    BigDecimal getDailyExposure();
}

