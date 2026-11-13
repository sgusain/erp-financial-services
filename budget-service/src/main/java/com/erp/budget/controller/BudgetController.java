package com.erp.budget.controller;

import com.erp.budget.approval.ApprovalDecisionRequest;
import com.erp.budget.approval.ApprovalTaskDto;
import com.erp.budget.approval.BudgetApprovalService;
import com.erp.budget.entity.Budget;
import com.erp.budget.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final BudgetApprovalService budgetApprovalService;

    @GetMapping
    public ResponseEntity<Page<Budget>> getAllBudgets(Pageable pageable) {
        return ResponseEntity.ok(budgetService.getAllBudgets(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Budget> getBudgetById(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.getBudgetById(id));
    }

    @PostMapping
    public ResponseEntity<Budget> createBudget(@Valid @RequestBody Budget budget) {
        Budget created = budgetApprovalService.requiresApproval(budget)
                ? budgetApprovalService.submitForApproval(budget)
                : budgetService.createBudget(budget);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/approvals")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<ApprovalTaskDto>> getOpenApprovalTasks() {
        return ResponseEntity.ok(budgetApprovalService.openTasks());
    }

    @PostMapping("/approvals/{taskId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> decide(@PathVariable String taskId,
                                       @Valid @RequestBody ApprovalDecisionRequest request) {
        budgetApprovalService.decide(taskId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(@PathVariable Long id, @Valid @RequestBody Budget budget) {
        return ResponseEntity.ok(budgetService.updateBudget(id, budget));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }

}
