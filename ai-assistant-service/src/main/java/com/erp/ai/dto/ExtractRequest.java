package com.erp.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExtractRequest(@NotBlank @Size(max = 2000) String text) {}
