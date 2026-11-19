package com.erp.ai.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CategorizeRequest(
        @NotBlank @Size(max = 500) String description,
        @NotNull @Positive BigDecimal amount) {}
