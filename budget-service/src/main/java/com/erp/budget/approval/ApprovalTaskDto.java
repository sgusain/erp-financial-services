package com.erp.budget.approval;

public record ApprovalTaskDto(String taskId, Long budgetId, String taskName, String candidateGroup) {
}
