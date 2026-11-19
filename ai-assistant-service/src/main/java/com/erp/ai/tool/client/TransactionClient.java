package com.erp.ai.tool.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "transaction-service", url = "${erp.clients.transaction-url:}")
public interface TransactionClient {

    @GetMapping("/api/transactions")
    PageResponse<TransactionView> list(@RequestHeader("Authorization") String auth,
                                       @RequestParam("page") int page, @RequestParam("size") int size,
                                       @RequestParam("sort") String sort);

    @GetMapping("/api/transactions/{id}")
    TransactionView get(@RequestHeader("Authorization") String auth, @PathVariable("id") Long id);
}
