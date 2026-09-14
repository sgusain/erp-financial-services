package com.erp.budget.service;

import com.erp.budget.entity.Budget;
import com.erp.budget.entity.BudgetStatus;
import com.erp.budget.entity.OutboxEvent;
import com.erp.budget.repository.BudgetRepository;
import com.erp.budget.repository.OutboxEventRepository;
import com.erp.common.event.BudgetEvent;
import com.erp.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(budgetRepository, outboxEventRepository, new ObjectMapper().findAndRegisterModules());
    }

    private Budget existingBudget() {
        Budget budget = new Budget();
        budget.setId(1L);
        budget.setBudgetName("Marketing");
        budget.setTotalAmount(new BigDecimal("1000.00"));
        budget.setSpentAmount(new BigDecimal("100.00"));
        budget.setFiscalYear(2026);
        budget.setStatus(BudgetStatus.ACTIVE);
        return budget;
    }

    @Test
    void updateBudget_setsExceededStatus_whenSpentReachesTotal() {
        Budget existing = existingBudget();
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Budget details = new Budget();
        details.setBudgetName("Marketing");
        details.setTotalAmount(new BigDecimal("1000.00"));
        details.setSpentAmount(new BigDecimal("1000.00"));
        details.setFiscalYear(2026);
        details.setStatus(BudgetStatus.ACTIVE);

        Budget result = budgetService.updateBudget(1L, details);

        assertThat(result.getStatus()).isEqualTo(BudgetStatus.EXCEEDED);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(outboxCaptor.capture());

        OutboxEvent saved = outboxCaptor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("Budget");
        assertThat(saved.getAggregateId()).isEqualTo("1");
        assertThat(saved.getEventType()).isEqualTo("BudgetExceeded");
        assertThat(saved.isPublished()).isFalse();

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        BudgetEvent event = assertDoesNotThrowDeserialize(mapper, saved.getPayload());
        assertThat(event.getBudgetId()).isEqualTo("1");
        assertThat(event.getBudgetName()).isEqualTo("Marketing");
        assertThat(event.getTotalAmount()).isEqualByComparingTo("1000.00");
        assertThat(event.getSpentAmount()).isEqualByComparingTo("1000.00");
        assertThat(event.getStatus()).isEqualTo("EXCEEDED");
    }

    @Test
    void updateBudget_setsExceededStatus_whenSpentGreaterThanTotal() {
        Budget existing = existingBudget();
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Budget details = new Budget();
        details.setBudgetName("Marketing");
        details.setTotalAmount(new BigDecimal("1000.00"));
        details.setSpentAmount(new BigDecimal("1500.00"));
        details.setFiscalYear(2026);
        details.setStatus(BudgetStatus.ACTIVE);

        Budget result = budgetService.updateBudget(1L, details);

        assertThat(result.getStatus()).isEqualTo(BudgetStatus.EXCEEDED);
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void updateBudget_doesNotExceed_whenSpentBelowTotal() {
        Budget existing = existingBudget();
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Budget details = new Budget();
        details.setBudgetName("Marketing");
        details.setTotalAmount(new BigDecimal("1000.00"));
        details.setSpentAmount(new BigDecimal("500.00"));
        details.setFiscalYear(2026);
        details.setStatus(BudgetStatus.ACTIVE);

        Budget result = budgetService.updateBudget(1L, details);

        assertThat(result.getStatus()).isEqualTo(BudgetStatus.ACTIVE);
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void updateBudget_throwsResourceNotFound_whenBudgetMissing() {
        when(budgetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.updateBudget(99L, existingBudget()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void getBudgetById_returnsBudget_whenFound() {
        Budget existing = existingBudget();
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(existing));

        Budget result = budgetService.getBudgetById(1L);

        assertThat(result).isEqualTo(existing);
    }

    @Test
    void getBudgetById_throwsResourceNotFound_whenMissing() {
        when(budgetRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.getBudgetById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void createBudget_delegatesToRepository() {
        Budget toCreate = existingBudget();
        toCreate.setId(null);
        Budget persisted = existingBudget();
        when(budgetRepository.save(toCreate)).thenReturn(persisted);

        Budget result = budgetService.createBudget(toCreate);

        assertThat(result).isEqualTo(persisted);
        verify(budgetRepository, times(1)).save(toCreate);
    }

    @Test
    void deleteBudget_removesExistingBudget() {
        Budget existing = existingBudget();
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(existing));

        budgetService.deleteBudget(1L);

        verify(budgetRepository, times(1)).delete(existing);
    }

    @Test
    void deleteBudget_throwsResourceNotFound_whenMissing() {
        when(budgetRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.deleteBudget(7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllBudgets_delegatesToRepository() {
        budgetService.getAllBudgets(org.springframework.data.domain.Pageable.unpaged());
        verify(budgetRepository, times(1)).findAll(org.springframework.data.domain.Pageable.unpaged());
    }

    private BudgetEvent assertDoesNotThrowDeserialize(ObjectMapper mapper, String payload) {
        try {
            return mapper.readValue(payload, BudgetEvent.class);
        } catch (Exception e) {
            throw new AssertionError("Failed to deserialize outbox payload back to BudgetEvent", e);
        }
    }
}
