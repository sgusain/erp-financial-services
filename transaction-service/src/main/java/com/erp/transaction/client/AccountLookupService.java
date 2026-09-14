package com.erp.transaction.client;

import com.erp.common.enums.TransactionType;
import com.erp.common.exception.BadRequestException;
import com.erp.common.exception.ResourceNotFoundException;
import com.erp.common.exception.ServiceUnavailableException;
import com.erp.transaction.dto.AccountResponse;
import com.erp.transaction.dto.BalanceAdjustmentRequest;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountLookupService {

    private static final String INSTANCE_NAME = "accountService";

    private final AccountClient accountClient;

    @Retry(name = INSTANCE_NAME, fallbackMethod = "fallback")
    @CircuitBreaker(name = INSTANCE_NAME)
    @RateLimiter(name = INSTANCE_NAME)
    public AccountResponse getAccount(Long accountId) {
        return accountClient.getAccountById(accountId);
    }

    private AccountResponse fallback(Long accountId, Throwable t) {
        if (t instanceof FeignException.NotFound) {
            throw new ResourceNotFoundException("Account not found with id: " + accountId);
        }
        if (t instanceof RequestNotPermitted) {
            throw (RequestNotPermitted) t;
        }
        throw new ServiceUnavailableException("Account service is unavailable, please try again later");
    }

    @Retry(name = INSTANCE_NAME, fallbackMethod = "adjustBalanceFallback")
    @CircuitBreaker(name = INSTANCE_NAME)
    @RateLimiter(name = INSTANCE_NAME)
    public AccountResponse adjustBalance(Long accountId, TransactionType type, BigDecimal amount) {
        return accountClient.updateBalance(accountId, new BalanceAdjustmentRequest(type, amount));
    }

    private AccountResponse adjustBalanceFallback(Long accountId, TransactionType type, BigDecimal amount, Throwable t) {
        if (t instanceof FeignException.NotFound) {
            throw new ResourceNotFoundException("Account not found with id: " + accountId);
        }
        if (t instanceof FeignException.BadRequest) {
            throw new BadRequestException("Unable to adjust balance for account id: " + accountId
                    + " - insufficient funds or invalid request");
        }
        if (t instanceof RequestNotPermitted) {
            throw (RequestNotPermitted) t;
        }
        throw new ServiceUnavailableException("Account service is unavailable, please try again later");
    }
}
