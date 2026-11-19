package com.erp.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PolicyQuestion(@NotBlank @Size(max = 1000) String question) {}
