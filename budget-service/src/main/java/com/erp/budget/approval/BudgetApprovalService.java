package com.erp.budget.approval;

import com.erp.budget.entity.Budget;
import com.erp.budget.entity.BudgetStatus;
import com.erp.budget.repository.BudgetRepository;
import com.erp.common.exception.ResourceNotFoundException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Starts and drives the Camunda "budgetApproval" process.
 * Budgets at or above the threshold are saved as PENDING_APPROVAL and wait for a manager.
 */
@Service
public class BudgetApprovalService {

    public static final String PROCESS_KEY = "budgetApproval";

    private final BudgetRepository budgetRepository;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final BigDecimal threshold;
    private final String escalationAfter;

    public BudgetApprovalService(BudgetRepository budgetRepository,
                                 RuntimeService runtimeService,
                                 TaskService taskService,
                                 @Value("${budget.approval.threshold:100000}") BigDecimal threshold,
                                 @Value("${budget.approval.escalation-after:PT48H}") String escalationAfter) {
        this.budgetRepository = budgetRepository;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.threshold = threshold;
        this.escalationAfter = escalationAfter;
    }

    public boolean requiresApproval(Budget budget) {
        return budget.getTotalAmount() != null && budget.getTotalAmount().compareTo(threshold) >= 0;
    }

    /** Saves the budget as PENDING_APPROVAL and starts the process, in one transaction. */
    @Transactional
    public Budget submitForApproval(Budget budget) {
        budget.setStatus(BudgetStatus.PENDING_APPROVAL);
        Budget saved = budgetRepository.save(budget);
        runtimeService.startProcessInstanceByKey(
                PROCESS_KEY,
                saved.getId().toString(),
                Map.of("escalationAfter", escalationAfter, "approved", false));
        return saved;
    }

    public List<ApprovalTaskDto> openTasks() {
        return taskService.createTaskQuery()
                .processDefinitionKey(PROCESS_KEY)
                .active()
                .orderByTaskCreateTime().asc()
                .list()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /** Completes the manager or finance-head task. The process then activates or rejects the budget. */
    @Transactional
    public void decide(String taskId, ApprovalDecisionRequest request) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new ResourceNotFoundException("Approval task not found with id: " + taskId);
        }
        if (request.comment() != null) {
            taskService.createComment(taskId, task.getProcessInstanceId(), request.comment());
        }
        taskService.complete(taskId, Map.of("approved", request.approved()));
    }

    private ApprovalTaskDto toDto(Task task) {
        String businessKey = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult()
                .getBusinessKey();
        String group = taskService.getIdentityLinksForTask(task.getId()).stream()
                .map(link -> link.getGroupId())
                .filter(g -> g != null)
                .findFirst()
                .orElse(null);
        return new ApprovalTaskDto(task.getId(), Long.valueOf(businessKey), task.getName(), group);
    }
}
