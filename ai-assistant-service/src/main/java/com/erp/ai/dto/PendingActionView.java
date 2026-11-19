package com.erp.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PendingActionView(
        String id,
        String actionType,
        Long budgetId,
        BigDecimal newTotalAmount,
        String reason,
        String status,
        LocalDateTime createdAt) {}
