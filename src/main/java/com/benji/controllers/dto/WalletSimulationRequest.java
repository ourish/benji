package com.benji.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record WalletSimulationRequest(
        @NotEmpty(message = "Assets list must not be empty")
        List<SimulatedAsset> assets
) {
    public record SimulatedAsset(
            @NotBlank(message = "Symbol must not be blank")
            String symbol,

            @NotNull(message = "Quantity must be provided")
            @Positive(message = "Quantity must be greater than zero")
            BigDecimal quantity,

            @NotNull(message = "Value must be provided")
            @Positive(message = "Value must be greater than zero")
            BigDecimal value
    ) {}
}


