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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountLookupService {

    private static final String INSTANCE_NAME = "accountService";

    // Must match the Redis key Spring's cache abstraction generates in account-service
    // for the "account-balance" cache (default format: "<cacheName>::<key>"), so this
    // cross-service eviction actually hits the same entry account-service reads.
    private static final String ACCOUNT_BALANCE_CACHE_PREFIX = "account-balance::";

    private final AccountClient accountClient;
    private final StringRedisTemplate redisTemplate;

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
        AccountResponse response = accountClient.updateBalance(accountId, new BalanceAdjustmentRequest(type, amount));
        // account-service's own @CacheEvict on applyBalanceChange already covers this
        // update (same JVM, same call), but this direct delete keeps the caches in sync
        // even if that in-process eviction is ever bypassed or the two services drift.
        redisTemplate.delete(ACCOUNT_BALANCE_CACHE_PREFIX + accountId);
        return response;
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
