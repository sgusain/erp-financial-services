package com.erp.ai.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import feign.FeignException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> denied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "You are not allowed to do this");
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ErrorResponse> rateLimit(RateLimitExceededException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(TokenBudgetExceededException.class)
    ResponseEntity<ErrorResponse> budget(TokenBudgetExceededException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(LlmUnavailableException.class)
    ResponseEntity<ErrorResponse> llm(LlmUnavailableException ex) {
        log.warn("LLM unavailable: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "The AI provider is not available right now. Try again later.");
    }

    /** An ERP service said no (for example 403 because the user is not ADMIN). We pass the same status on. */
    @ExceptionHandler(FeignException.class)
    ResponseEntity<ErrorResponse> erp(FeignException ex) {
        HttpStatus status = ex.status() > 0 ? HttpStatus.resolve(ex.status()) : HttpStatus.BAD_GATEWAY;
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        log.warn("ERP service call failed: {}", ex.getMessage());
        return build(status, "ERP service error (" + ex.status() + ")");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> badInput(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> other(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), status.getReasonPhrase(), message));
    }
}
