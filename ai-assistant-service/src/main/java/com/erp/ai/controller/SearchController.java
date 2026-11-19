package com.erp.ai.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.ai.dto.SearchHit;
import com.erp.ai.security.CurrentUser;
import com.erp.ai.service.TransactionSearchService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/ai")
@Validated
public class SearchController {

    private final TransactionSearchService searchService;

    public SearchController(TransactionSearchService searchService) {
        this.searchService = searchService;
    }

    /** Admin only because it spends embedding tokens. */
    @PostMapping("/index/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> index(@RequestParam(defaultValue = "90") @Min(1) @Max(365) int days) {
        return ResponseEntity.ok(Map.of("indexed", searchService.index(CurrentUser.bearerToken(), days)));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SearchHit>> search(@RequestParam("q") @NotBlank @Size(max = 200) String query,
                                                  @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
        List<SearchHit> hits = searchService.search(query, limit).stream()
                .map(d -> new SearchHit(String.valueOf(d.getMetadata().get("sourceId")),
                        String.valueOf(d.getMetadata().get("accountId")), d.getText()))
                .toList();
        return ResponseEntity.ok(hits);
    }
}
