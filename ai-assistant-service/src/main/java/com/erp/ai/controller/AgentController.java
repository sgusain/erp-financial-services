package com.erp.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.ai.agent.BudgetAnalystAgent;
import com.erp.ai.dto.AgentRequest;
import com.erp.ai.dto.AgentResult;
import com.erp.ai.security.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai/agents")
public class AgentController {

    private final BudgetAnalystAgent agent;

    public AgentController(BudgetAnalystAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/budget-analyst")
    public ResponseEntity<AgentResult> budgetAnalyst(@Valid @RequestBody AgentRequest request) {
        return ResponseEntity.ok(agent.run(CurrentUser.username(), CurrentUser.bearerToken(), request.fiscalYear()));
    }
}
