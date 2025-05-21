package com.benji.controllers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateWalletRequest(
        @NotBlank(message = "Request is empty or blank!")
        @Email(message = "Must be a valid email address")
        String email
) {}

