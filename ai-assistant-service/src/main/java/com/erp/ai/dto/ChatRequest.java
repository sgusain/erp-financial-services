package com.erp.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** chatId is optional. Leave it empty to start a new chat. */
public record ChatRequest(
        @NotBlank @Size(max = 2000) String message,
        String chatId) {}
