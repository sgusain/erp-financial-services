package com.erp.ai.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.ai.dto.CategorizeRequest;
import com.erp.ai.dto.ExplainResponse;
import com.erp.ai.dto.ExtractRequest;
import com.erp.ai.dto.TransactionCategory;
import com.erp.ai.security.CurrentUser;
import com.erp.ai.service.TransactionAiService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai/transactions")
public class TransactionAiController {

    private final TransactionAiService service;

    public TransactionAiController(TransactionAiService service) {
        this.service = service;
    }

    @PostMapping("/{id}/explain")
    public ResponseEntity<ExplainResponse> explain(@PathVariable Long id) {
        return ResponseEntity.ok(service.explain(CurrentUser.username(), CurrentUser.bearerToken(), id));
    }

    @PostMapping("/categorize")
    public ResponseEntity<TransactionCategory> categorize(@Valid @RequestBody CategorizeRequest request) {
        return ResponseEntity.ok(service.categorize(CurrentUser.username(), request.description(), request.amount()));
    }

    @PostMapping("/tags")
    public ResponseEntity<List<String>> tags(@Valid @RequestBody CategorizeRequest request) {
        return ResponseEntity.ok(service.tags(CurrentUser.username(), request.description(), request.amount()));
    }

    @PostMapping("/extract")
    public ResponseEntity<Map<String, String>> extract(@Valid @RequestBody ExtractRequest request) {
        return ResponseEntity.ok(service.extract(CurrentUser.username(), request.text()));
    }
}
