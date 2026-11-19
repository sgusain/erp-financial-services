package com.erp.ai.tool.client;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Dates are kept as text so a different date format in the ERP can never break parsing. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionView(Long id, Long accountId, BigDecimal amount, String type,
                              String description, String transactionDate) {}
