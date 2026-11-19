package com.erp.ai.tool.client;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountView(Long id, String accountCode, String accountName, String accountType, BigDecimal balance) {}
