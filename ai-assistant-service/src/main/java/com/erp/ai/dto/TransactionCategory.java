package com.erp.ai.dto;

/** Structured output: Spring AI asks the model for JSON in this shape and converts it for us. */
public record TransactionCategory(Category category, double confidence, String reason) {

    public enum Category {
        OFFICE_SUPPLIES, TRAVEL, SOFTWARE, PAYROLL, UTILITIES, MARKETING, TAX, OTHER
    }
}
