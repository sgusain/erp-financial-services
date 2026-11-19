package com.erp.ai.tool.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "budget-service", url = "${erp.clients.budget-url:}")
public interface BudgetClient {

    @GetMapping("/api/budgets")
    PageResponse<BudgetView> list(@RequestHeader("Authorization") String auth,
                                  @RequestParam("page") int page, @RequestParam("size") int size);

    @GetMapping("/api/budgets/{id}")
    BudgetView get(@RequestHeader("Authorization") String auth, @PathVariable("id") Long id);

    @PutMapping("/api/budgets/{id}")
    BudgetView update(@RequestHeader("Authorization") String auth, @PathVariable("id") Long id,
                      @RequestBody BudgetView budget);
}
