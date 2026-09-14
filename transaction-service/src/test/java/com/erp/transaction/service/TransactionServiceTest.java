package com.erp.transaction.service;

import com.erp.common.enums.TransactionType;
import com.erp.common.event.TransactionEvent;
import com.erp.common.exception.BadRequestException;
import com.erp.common.exception.ResourceNotFoundException;
import com.erp.transaction.client.AccountLookupService;
import com.erp.transaction.dto.AccountResponse;
import com.erp.transaction.entity.OutboxEvent;
import com.erp.transaction.entity.Transaction;
import com.erp.transaction.repository.OutboxEventRepository;
import com.erp.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private AccountLookupService accountLookupService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository, outboxEventRepository, accountLookupService,
                new ObjectMapper().findAndRegisterModules());
    }

    private Transaction newTransaction() {
        Transaction t = new Transaction();
        t.setAccountId(10L);
        t.setAmount(new BigDecimal("50.00"));
        t.setType(TransactionType.DEBIT);
        t.setDescription("test txn");
        return t;
    }

    @Test
    void createTransaction_checksAccountBeforeAdjustingBalance_inOrder() {
        Transaction toSave = newTransaction();
        Transaction saved = newTransaction();
        saved.setId(1L);

        when(accountLookupService.getAccount(10L)).thenReturn(new AccountResponse());
        when(transactionRepository.save(toSave)).thenReturn(saved);
        when(accountLookupService.adjustBalance(10L, TransactionType.DEBIT, new BigDecimal("50.00")))
                .thenReturn(new AccountResponse());

        transactionService.createTransaction(toSave);

        InOrder inOrder = inOrder(accountLookupService, transactionRepository);
        inOrder.verify(accountLookupService).getAccount(10L);
        inOrder.verify(transactionRepository).save(toSave);
        inOrder.verify(accountLookupService).adjustBalance(10L, TransactionType.DEBIT, new BigDecimal("50.00"));
    }

    @Test
    void createTransaction_savesOutboxEvent_withCorrectAggregateAndEventType() {
        Transaction toSave = newTransaction();
        Transaction saved = newTransaction();
        saved.setId(7L);

        when(accountLookupService.getAccount(10L)).thenReturn(new AccountResponse());
        when(transactionRepository.save(toSave)).thenReturn(saved);
        when(accountLookupService.adjustBalance(eq(10L), any(), any())).thenReturn(new AccountResponse());

        transactionService.createTransaction(toSave);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(captor.capture());

        OutboxEvent event = captor.getValue();
        assertThat(event.getAggregateType()).isEqualTo("Transaction");
        assertThat(event.getAggregateId()).isEqualTo("7");
        assertThat(event.getEventType()).isEqualTo("TransactionCreated");
        assertThat(event.isPublished()).isFalse();

        try {
            TransactionEvent payload = new ObjectMapper().findAndRegisterModules()
                    .readValue(event.getPayload(), TransactionEvent.class);
            assertThat(payload.getTransactionId()).isEqualTo("7");
            assertThat(payload.getAccountId()).isEqualTo("10");
            assertThat(payload.getType()).isEqualTo("DEBIT");
        } catch (Exception e) {
            throw new AssertionError("outbox payload did not deserialize back to TransactionEvent", e);
        }
    }

    @Test
    void createTransaction_doesNotSaveOrAdjust_whenAccountNotFound() {
        Transaction toSave = newTransaction();
        when(accountLookupService.getAccount(10L)).thenThrow(new ResourceNotFoundException("Account not found with id: 10"));

        assertThatThrownBy(() -> transactionService.createTransaction(toSave))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).save(any());
        verify(accountLookupService, never()).adjustBalance(any(), any(), any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void createTransaction_propagatesBadRequest_whenAdjustBalanceFails() {
        Transaction toSave = newTransaction();
        Transaction saved = newTransaction();
        saved.setId(3L);

        when(accountLookupService.getAccount(10L)).thenReturn(new AccountResponse());
        when(transactionRepository.save(toSave)).thenReturn(saved);
        when(accountLookupService.adjustBalance(eq(10L), any(), any()))
                .thenThrow(new BadRequestException("insufficient funds"));

        assertThatThrownBy(() -> transactionService.createTransaction(toSave))
                .isInstanceOf(BadRequestException.class);

        // The transaction row itself was still handed to the mock repository (this test
        // uses a mocked repository so it cannot demonstrate real rollback - that
        // requires a live transaction manager, verified separately in
        // TransactionControllerIntegrationTest#createTransaction_rollsBackTransactionRow_whenAdjustBalanceFailsAfterSave).
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void getTransactionById_throwsResourceNotFound_whenMissing() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTransactionById_returnsTransaction_whenFound() {
        Transaction t = newTransaction();
        t.setId(5L);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(t));

        assertThat(transactionService.getTransactionById(5L)).isEqualTo(t);
    }

    @Test
    void deleteTransaction_removesExistingTransaction() {
        Transaction t = newTransaction();
        t.setId(2L);
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(t));

        transactionService.deleteTransaction(2L);

        verify(transactionRepository, times(1)).delete(t);
    }

    @Test
    void updateTransaction_updatesFieldsAndSaves() {
        Transaction existing = newTransaction();
        existing.setId(4L);
        when(transactionRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction details = newTransaction();
        details.setAmount(new BigDecimal("99.00"));
        details.setType(TransactionType.CREDIT);
        details.setDescription("updated");

        Transaction result = transactionService.updateTransaction(4L, details);

        assertThat(result.getAmount()).isEqualByComparingTo("99.00");
        assertThat(result.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(result.getDescription()).isEqualTo("updated");
    }
}
