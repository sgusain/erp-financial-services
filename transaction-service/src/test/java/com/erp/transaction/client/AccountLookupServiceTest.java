package com.erp.transaction.client;

import com.erp.common.enums.TransactionType;
import com.erp.common.exception.BadRequestException;
import com.erp.common.exception.ResourceNotFoundException;
import com.erp.common.exception.ServiceUnavailableException;
import com.erp.transaction.dto.AccountResponse;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the fallback/exception-mapping methods on AccountLookupService.
 * The Resilience4j @CircuitBreaker/@Retry/@RateLimiter annotations themselves need a
 * live Spring AOP proxy to actually engage, so their state-machine behavior is left to
 * the TransactionControllerIntegrationTest (full Spring context); this class instead
 * verifies, via reflection, that the private fallback methods map each failure type to
 * the exception the rest of the app expects.
 */
@ExtendWith(MockitoExtension.class)
class AccountLookupServiceTest {

    @Mock
    private AccountClient accountClient;

    // Fallback methods are private, so we invoke them via reflection on a real instance
    // constructed with a mocked AccountClient. Built in @BeforeEach (not a field
    // initializer) since @Mock fields aren't populated until MockitoExtension runs.
    private AccountLookupService realService;

    @BeforeEach
    void setUp() {
        realService = new AccountLookupService(accountClient);
    }

    private FeignException notFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/accounts/1",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        return new FeignException.NotFound("not found", request, null, null);
    }

    private FeignException badRequest() {
        Request request = Request.create(Request.HttpMethod.PUT, "/api/accounts/1/balance",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        return new FeignException.BadRequest("bad request", request, null, null);
    }

    private Object invokeFallback(String methodName, Class<?>[] paramTypes, Object[] args) throws Exception {
        Method method = AccountLookupService.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        try {
            return method.invoke(realService, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    void getAccountFallback_mapsNotFound_toResourceNotFoundException() {
        assertThatThrownBy(() -> invokeFallback("fallback", new Class<?>[]{Long.class, Throwable.class},
                new Object[]{1L, notFound()}))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void getAccountFallback_mapsRequestNotPermitted_toRequestNotPermitted() {
        RequestNotPermitted rnp = RequestNotPermitted.createRequestNotPermitted(
                io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("accountService"));

        assertThatThrownBy(() -> invokeFallback("fallback", new Class<?>[]{Long.class, Throwable.class},
                new Object[]{1L, rnp}))
                .isInstanceOf(RequestNotPermitted.class);
    }

    @Test
    void getAccountFallback_mapsOtherFailures_toServiceUnavailable() {
        assertThatThrownBy(() -> invokeFallback("fallback", new Class<?>[]{Long.class, Throwable.class},
                new Object[]{1L, new RuntimeException("boom")}))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void adjustBalanceFallback_mapsNotFound_toResourceNotFoundException() {
        assertThatThrownBy(() -> invokeFallback("adjustBalanceFallback",
                new Class<?>[]{Long.class, TransactionType.class, BigDecimal.class, Throwable.class},
                new Object[]{1L, TransactionType.DEBIT, BigDecimal.TEN, notFound()}))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adjustBalanceFallback_mapsBadRequest_toBadRequestException_insufficientFunds() {
        assertThatThrownBy(() -> invokeFallback("adjustBalanceFallback",
                new Class<?>[]{Long.class, TransactionType.class, BigDecimal.class, Throwable.class},
                new Object[]{1L, TransactionType.DEBIT, BigDecimal.TEN, badRequest()}))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("insufficient funds");
    }

    @Test
    void adjustBalanceFallback_mapsRequestNotPermitted_toRequestNotPermitted() {
        RequestNotPermitted rnp = RequestNotPermitted.createRequestNotPermitted(
                io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("accountService"));

        assertThatThrownBy(() -> invokeFallback("adjustBalanceFallback",
                new Class<?>[]{Long.class, TransactionType.class, BigDecimal.class, Throwable.class},
                new Object[]{1L, TransactionType.CREDIT, BigDecimal.ONE, rnp}))
                .isInstanceOf(RequestNotPermitted.class);
    }

    @Test
    void adjustBalanceFallback_mapsOtherFailures_toServiceUnavailable() {
        assertThatThrownBy(() -> invokeFallback("adjustBalanceFallback",
                new Class<?>[]{Long.class, TransactionType.class, BigDecimal.class, Throwable.class},
                new Object[]{1L, TransactionType.CREDIT, BigDecimal.ONE, new IllegalStateException("down")}))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void getAccount_delegatesToClient() {
        AccountResponse response = new AccountResponse();
        response.setId(1L);
        org.mockito.Mockito.when(accountClient.getAccountById(1L)).thenReturn(response);

        AccountResponse result = realService.getAccount(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }
}
