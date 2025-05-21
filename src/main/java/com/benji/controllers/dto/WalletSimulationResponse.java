package com.benji.controllers.dto;

import java.math.BigDecimal;

public record WalletSimulationResponse(
        BigDecimal total,
        String bestAsset,
        BigDecimal bestPerformance,
        String worstAsset,
        BigDecimal worstPerformance
) {
}

