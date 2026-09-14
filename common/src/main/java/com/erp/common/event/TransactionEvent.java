package com.erp.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEvent {
    private String transactionId;
    private String accountId;
    private String type;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}
