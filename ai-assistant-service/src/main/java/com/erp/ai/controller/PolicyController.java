package com.erp.ai.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.ai.dto.PolicyAnswer;
import com.erp.ai.dto.PolicyQuestion;
import com.erp.ai.security.CurrentUser;
import com.erp.ai.service.PolicyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai/policy")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping("/ask")
    public ResponseEntity<PolicyAnswer> ask(@Valid @RequestBody PolicyQuestion request) {
        return ResponseEntity.ok(policyService.ask(CurrentUser.username(), request.question()));
    }

    /** Reads the policy files again and rebuilds the vector store. Admin only. */
    @PostMapping("/ingest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> ingest() {
        return ResponseEntity.ok(Map.of("chunks", policyService.ingest()));
    }
}
