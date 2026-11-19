package com.erp.ai.tool.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/** Calls account-service with the user's own token, so ERP rules and roles still apply. */
@FeignClient(name = "account-service", url = "${erp.clients.account-url:}")
public interface AccountClient {

    @GetMapping("/api/accounts")
    PageResponse<AccountView> list(@RequestHeader("Authorization") String auth,
                                   @RequestParam("page") int page, @RequestParam("size") int size);

    @GetMapping("/api/accounts/{id}")
    AccountView get(@RequestHeader("Authorization") String auth, @PathVariable("id") Long id);
}
