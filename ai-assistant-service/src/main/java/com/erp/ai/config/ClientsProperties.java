package com.erp.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Optional fixed URLs for the ERP services. Empty means: find them through Eureka. */
@ConfigurationProperties(prefix = "erp.clients")
public record ClientsProperties(String accountUrl, String transactionUrl, String budgetUrl, String userUrl) {}
