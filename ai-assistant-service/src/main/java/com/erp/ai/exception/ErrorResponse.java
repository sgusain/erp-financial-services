package com.erp.ai.exception;

import java.time.LocalDateTime;

/** Same shape of error body as the other services. */
public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now());
    }
}
