package com.erp.budget.approval;

import com.erp.budget.entity.Budget;
import com.erp.budget.repository.BudgetRepository;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BudgetApprovalServiceTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private TaskService taskService;

    private BudgetApprovalService service() {
        return new BudgetApprovalService(budgetRepository, runtimeService, taskService,
                new BigDecimal("100000"), "PT48H");
    }

    private Budget budgetOf(String amount) {
        Budget budget = new Budget();
        budget.setTotalAmount(new BigDecimal(amount));
        return budget;
    }

    @Test
    void requiresApproval_trueAtOrAboveThreshold() {
        assertThat(service().requiresApproval(budgetOf("100000.00"))).isTrue();
        assertThat(service().requiresApproval(budgetOf("250000.00"))).isTrue();
    }

    @Test
    void requiresApproval_falseBelowThreshold() {
        assertThat(service().requiresApproval(budgetOf("99999.99"))).isFalse();
    }
}
