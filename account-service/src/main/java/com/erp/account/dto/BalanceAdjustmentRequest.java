package com.erp.account.dto;

import com.erp.common.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceAdjustmentRequest {

    @NotNull
    private TransactionType type;

    @NotNull
    @Positive
    private BigDecimal amount;
}
