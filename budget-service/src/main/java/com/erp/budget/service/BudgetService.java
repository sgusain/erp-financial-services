package com.erp.budget.service;

import com.erp.budget.entity.Budget;
import com.erp.budget.entity.BudgetStatus;
import com.erp.budget.entity.OutboxEvent;
import com.erp.budget.repository.BudgetRepository;
import com.erp.budget.repository.OutboxEventRepository;
import com.erp.common.event.BudgetEvent;
import com.erp.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public Page<Budget> getAllBudgets(Pageable pageable) {
        return budgetRepository.findAll(pageable);
    }

    public Budget getBudgetById(Long id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
    }

    public Budget createBudget(Budget budget) {
        return budgetRepository.save(budget);
    }

    @Transactional
    public Budget updateBudget(Long id, Budget budgetDetails) {
        Budget budget = getBudgetById(id);
        budget.setBudgetName(budgetDetails.getBudgetName());
        budget.setTotalAmount(budgetDetails.getTotalAmount());
        budget.setSpentAmount(budgetDetails.getSpentAmount());
        budget.setFiscalYear(budgetDetails.getFiscalYear());
        budget.setStatus(budgetDetails.getStatus());
        Budget saved = budgetRepository.save(budget);

        if (saved.getSpentAmount().compareTo(saved.getTotalAmount()) >= 0) {
            saved.setStatus(BudgetStatus.EXCEEDED);
            budgetRepository.save(saved);

            BudgetEvent event = new BudgetEvent(
                    saved.getId().toString(),
                    saved.getBudgetName(),
                    saved.getTotalAmount(),
                    saved.getSpentAmount(),
                    saved.getStatus().name(),
                    LocalDateTime.now()
            );

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType("Budget");
            outboxEvent.setAggregateId(saved.getId().toString());
            outboxEvent.setEventType("BudgetExceeded");
            outboxEvent.setPayload(serialize(event));
            outboxEvent.setCreatedAt(LocalDateTime.now());
            outboxEventRepository.save(outboxEvent);
        }

        return saved;
    }

    private String serialize(BudgetEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize BudgetEvent for outbox", e);
        }
    }

    public void deleteBudget(Long id) {
        Budget budget = getBudgetById(id);
        budgetRepository.delete(budget);
    }
}