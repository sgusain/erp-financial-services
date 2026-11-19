package com.erp.ai.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.erp.ai.service.ErpDataService;
import com.erp.ai.service.PendingActionService;

class BudgetAnalystToolsTest {

    private final ErpDataService erp = mock(ErpDataService.class);
    private final PendingActionService pending = mock(PendingActionService.class);

    @Test
    void stopsCallingTheErpAfterTheStepLimit() {
        when(erp.budgetStatus("Bearer t", 2026)).thenReturn(List.of());
        BudgetAnalystTools tools = new BudgetAnalystTools(erp, pending, "Bearer t", "sandy", 2);

        tools.getBudgetStatus(2026);
        tools.getBudgetStatus(2026);
        Object third = tools.getBudgetStatus(2026);

        assertThat(third).isEqualTo(BudgetAnalystTools.LIMIT_REACHED);
        assertThat(tools.hitLimit()).isTrue();
        assertThat(tools.stepsUsed()).isEqualTo(2);
        verify(erp, times(2)).budgetStatus("Bearer t", 2026); // the third call never reached the ERP
    }

    @Test
    void staysUnderTheLimitForShortRuns() {
        BudgetAnalystTools tools = new BudgetAnalystTools(erp, pending, "Bearer t", "sandy", 5);

        tools.getBudgetStatus(2026);

        assertThat(tools.hitLimit()).isFalse();
        assertThat(tools.stepsUsed()).isEqualTo(1);
    }
}
