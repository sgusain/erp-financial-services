package com.erp.budget.approval;

import com.erp.budget.entity.Budget;
import com.erp.budget.entity.BudgetStatus;
import com.erp.budget.repository.BudgetRepository;
import com.erp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/** Service task: runs when the budget is approved. Process business key = budget id. */
@Component("approveBudgetDelegate")
@RequiredArgsConstructor
public class ApproveBudgetDelegate implements JavaDelegate {

    private final BudgetRepository budgetRepository;

    @Override
    public void execute(DelegateExecution execution) {
        Long budgetId = Long.valueOf(execution.getProcessBusinessKey());
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));
        budget.setStatus(BudgetStatus.ACTIVE);
        budgetRepository.save(budget);
    }
}
