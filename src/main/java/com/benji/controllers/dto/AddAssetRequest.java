package com.benji.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;


public record AddAssetRequest(
        @NotBlank(message = "Symbol must not be blank")
        String symbol,

        @NotNull(message = "Quantity must be provided")
        @Positive(message = "Quantity must be greater than zero")
        BigDecimal quantity
) {}

