package com.erp.ai.exception;

public class TokenBudgetExceededException extends RuntimeException {
    public TokenBudgetExceededException(String message) {
        super(message);
    }
}
