package com.erp.ai.service;

import java.util.function.Supplier;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

import com.erp.ai.exception.LlmUnavailableException;

/** Every blocking model call goes through here, so the circuit breaker and timeout apply everywhere. */
@Component
public class LlmGateway {

    private final CircuitBreakerFactory<?, ?> circuitBreakers;

    public LlmGateway(CircuitBreakerFactory<?, ?> circuitBreakers) {
        this.circuitBreakers = circuitBreakers;
    }

    public <T> T call(Supplier<T> modelCall) {
        return circuitBreakers.create("llm").run(modelCall, error -> {
            throw new LlmUnavailableException("Model call failed: " + error.getMessage(), error);
        });
    }
}
