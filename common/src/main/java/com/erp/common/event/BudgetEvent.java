package com.erp.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetEvent {
    private String budgetId;
    private String budgetName;
    private BigDecimal totalAmount;
    private BigDecimal spentAmount;
    private String status;
    private LocalDateTime timestamp;
}
