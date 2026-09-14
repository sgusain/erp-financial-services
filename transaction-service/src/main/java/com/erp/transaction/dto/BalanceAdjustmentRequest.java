package com.erp.transaction.dto;

import com.erp.common.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceAdjustmentRequest {

    private TransactionType type;

    private BigDecimal amount;
}
