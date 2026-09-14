package com.erp.transaction.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountResponse {
    private Long id;
    private String accountCode;
    private String accountName;
    private String accountType;
    private BigDecimal balance;
}
