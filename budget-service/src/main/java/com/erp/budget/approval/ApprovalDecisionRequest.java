package com.erp.budget.approval;

import jakarta.validation.constraints.NotNull;

public record ApprovalDecisionRequest(@NotNull Boolean approved, String comment) {
}
