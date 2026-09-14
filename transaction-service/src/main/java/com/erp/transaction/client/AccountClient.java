package com.erp.transaction.client;

import com.erp.transaction.dto.AccountResponse;
import com.erp.transaction.dto.BalanceAdjustmentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ACCOUNT-SERVICE")
public interface AccountClient {

    @GetMapping("/api/accounts/{id}")
    AccountResponse getAccountById(@PathVariable("id") Long id);

    @PutMapping("/api/accounts/{id}/balance")
    AccountResponse updateBalance(@PathVariable("id") Long id, @RequestBody BalanceAdjustmentRequest request);
}
