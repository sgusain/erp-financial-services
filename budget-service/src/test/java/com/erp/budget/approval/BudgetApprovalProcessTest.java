package com.erp.budget.approval;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the BPMN on a plain in-memory H2 engine (no Spring, no Docker).
 * Service tasks are replaced by recording delegates.
 */
class BudgetApprovalProcessTest {

    private ProcessEngine engine;
    private final List<String> approvedBudgets = new CopyOnWriteArrayList<>();
    private final List<String> rejectedBudgets = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        Map<Object, Object> beans = new HashMap<>();
        beans.put("approveBudgetDelegate", (JavaDelegate) e -> approvedBudgets.add(e.getProcessBusinessKey()));
        beans.put("rejectBudgetDelegate", (JavaDelegate) e -> rejectedBudgets.add(e.getProcessBusinessKey()));

        StandaloneInMemProcessEngineConfiguration config = new StandaloneInMemProcessEngineConfiguration();
        config.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        config.setJobExecutorActivate(false);
        config.setBeans(beans);
        engine = config.buildProcessEngine();

        engine.getRepositoryService().createDeployment()
                .addClasspathResource("processes/budget-approval.bpmn")
                .deploy();
    }

    @AfterEach
    void tearDown() {
        engine.close();
    }

    private void start(String budgetId) {
        engine.getRuntimeService().startProcessInstanceByKey(
                BudgetApprovalService.PROCESS_KEY, budgetId,
                Map.of("escalationAfter", "PT48H", "approved", false));
    }

    private Task onlyTask() {
        return engine.getTaskService().createTaskQuery().singleResult();
    }

    @Test
    void managerApproves_budgetIsActivated() {
        start("1");

        Task task = onlyTask();
        assertThat(task.getName()).isEqualTo("Manager approval");
        engine.getTaskService().complete(task.getId(), Map.of("approved", true));

        assertThat(approvedBudgets).containsExactly("1");
        assertThat(rejectedBudgets).isEmpty();
        assertThat(engine.getRuntimeService().createProcessInstanceQuery().count()).isZero();
    }

    @Test
    void managerRejects_budgetIsRejected() {
        start("2");

        engine.getTaskService().complete(onlyTask().getId(), Map.of("approved", false));

        assertThat(rejectedBudgets).containsExactly("2");
        assertThat(approvedBudgets).isEmpty();
    }

    @Test
    void managerTimesOut_escalatesToFinanceHead_whoApproves() {
        start("3");

        // Fire the boundary timer instead of waiting 48 hours
        Job timer = engine.getManagementService().createJobQuery().timers().singleResult();
        engine.getManagementService().executeJob(timer.getId());

        Task escalated = onlyTask();
        assertThat(escalated.getName()).isEqualTo("Finance head approval (escalated)");
        engine.getTaskService().complete(escalated.getId(), Map.of("approved", true));

        assertThat(approvedBudgets).containsExactly("3");
    }
}
