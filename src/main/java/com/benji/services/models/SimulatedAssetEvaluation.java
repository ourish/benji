package com.benji.services.models;

import java.math.BigDecimal;

public record SimulatedAssetEvaluation(
        String symbol,
        BigDecimal currentValue,
        BigDecimal performance,
        BigDecimal currentWorth) {
}
