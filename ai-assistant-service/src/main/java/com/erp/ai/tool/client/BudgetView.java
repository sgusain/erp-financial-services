package com.erp.ai.tool.client;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Used to read a budget and, with a new totalAmount, to send it back (the ERP update needs the full budget). */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BudgetView(Long id, String budgetName, BigDecimal totalAmount, BigDecimal spentAmount,
                         Integer fiscalYear, String status) {

    public BudgetView withTotalAmount(BigDecimal newTotal) {
        return new BudgetView(id, budgetName, newTotal, spentAmount, fiscalYear, status);
    }
}
