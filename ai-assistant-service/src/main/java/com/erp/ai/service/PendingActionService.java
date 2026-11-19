package com.erp.ai.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.ai.dto.PendingActionView;
import com.erp.ai.exception.NotFoundException;

/**
 * Write actions the assistant proposes. Two safety rules:
 * 1. Nothing changes until the user confirms.
 * 2. When confirmed, we call the ERP with the user's own token, so ERP roles still decide.
 */
@Service
public class PendingActionService {

    private static final String BUDGET_CHANGE = "BUDGET_CHANGE";

    private final JdbcClient jdbc;
    private final ErpDataService erp;

    public PendingActionService(JdbcClient jdbc, ErpDataService erp) {
        this.jdbc = jdbc;
        this.erp = erp;
    }

    public String create(String username, Long budgetId, BigDecimal newTotal, String reason) {
        if (newTotal == null || newTotal.signum() <= 0) {
            throw new IllegalArgumentException("newTotalAmount must be greater than zero");
        }
        String id = UUID.randomUUID().toString();
        jdbc.sql("""
                INSERT INTO pending_action (id, username, action_type, budget_id, new_total_amount, reason)
                VALUES (CAST(:id AS uuid), :username, :type, :budgetId, :amount, :reason)
                """)
                .param("id", id)
                .param("username", username)
                .param("type", BUDGET_CHANGE)
                .param("budgetId", budgetId)
                .param("amount", newTotal)
                .param("reason", truncate(reason, 500))
                .update();
        return id;
    }

    public Page<PendingActionView> listPending(String username, Pageable pageable) {
        List<PendingActionView> rows = jdbc.sql("""
                SELECT id, action_type, budget_id, new_total_amount, reason, status, created_at
                FROM pending_action WHERE username = :username AND status = 'PENDING'
                ORDER BY created_at DESC LIMIT :limit OFFSET :offset
                """)
                .param("username", username)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query(PendingActionService::toView)
                .list();
        long total = jdbc.sql("SELECT count(*) FROM pending_action WHERE username = :username AND status = 'PENDING'")
                .param("username", username).query(Long.class).single();
        return new PageImpl<>(rows, pageable, total);
    }

    private static PendingActionView toView(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new PendingActionView(
                rs.getString("id"), rs.getString("action_type"), rs.getLong("budget_id"),
                rs.getBigDecimal("new_total_amount"), rs.getString("reason"), rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    /** Applies the change in budget-service. Only the user who asked for it can confirm it. */
    @Transactional
    public PendingActionView confirm(String username, String id, String token) {
        PendingActionView action = findPending(username, id);
        erp.updateBudgetTotal(token, action.budgetId(), action.newTotalAmount());
        jdbc.sql("UPDATE pending_action SET status = 'CONFIRMED', decided_at = now() WHERE id = CAST(:id AS uuid)")
                .param("id", id).update();
        return new PendingActionView(action.id(), action.actionType(), action.budgetId(),
                action.newTotalAmount(), action.reason(), "CONFIRMED", action.createdAt());
    }

    @Transactional
    public PendingActionView reject(String username, String id) {
        PendingActionView action = findPending(username, id);
        jdbc.sql("UPDATE pending_action SET status = 'REJECTED', decided_at = now() WHERE id = CAST(:id AS uuid)")
                .param("id", id).update();
        return new PendingActionView(action.id(), action.actionType(), action.budgetId(),
                action.newTotalAmount(), action.reason(), "REJECTED", action.createdAt());
    }

    private PendingActionView findPending(String username, String id) {
        UUID.fromString(id); // 400 if it is not a UUID
        return jdbc.sql("""
                SELECT id, action_type, budget_id, new_total_amount, reason, status, created_at
                FROM pending_action
                WHERE id = CAST(:id AS uuid) AND username = :username AND status = 'PENDING'
                """)
                .param("id", id)
                .param("username", username)
                .query(PendingActionService::toView)
                .optional()
                .orElseThrow(() -> new NotFoundException("No pending action " + id + " for you"));
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
