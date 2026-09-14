package com.erp.transaction.service;

import com.erp.transaction.client.AccountLookupService;
import com.erp.transaction.entity.OutboxEvent;
import com.erp.transaction.entity.Transaction;
import com.erp.common.event.TransactionEvent;
import com.erp.common.exception.ResourceNotFoundException;
import com.erp.transaction.repository.OutboxEventRepository;
import com.erp.transaction.repository.TransactionRepository;
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
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountLookupService accountLookupService;
    private final ObjectMapper objectMapper;

    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    // @Transactional here means: if adjustBalance throws (account gone, insufficient
    // funds, account-service down), the local transactionRepository.save above is rolled
    // back too - verified empirically, no orphaned transaction row is left behind. The
    // remaining gap is narrower than a full dual-write problem: it's only the window
    // between adjustBalance succeeding remotely and this method's local commit, plus the
    // Kafka publish afterward being best-effort. Closing that fully needs an outbox/saga
    // pattern, tracked as follow-up work; it is not the common-case failure mode.
    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        accountLookupService.getAccount(transaction.getAccountId());

        Transaction saved = transactionRepository.save(transaction);

        accountLookupService.adjustBalance(saved.getAccountId(), saved.getType(), saved.getAmount());

        TransactionEvent event = new TransactionEvent(
                saved.getId().toString(),
                saved.getAccountId().toString(),
                saved.getType().name(),
                saved.getAmount(),
                LocalDateTime.now()
        );

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType("Transaction");
        outboxEvent.setAggregateId(saved.getId().toString());
        outboxEvent.setEventType("TransactionCreated");
        outboxEvent.setPayload(serialize(event));
        outboxEvent.setCreatedAt(LocalDateTime.now());
        outboxEventRepository.save(outboxEvent);

        return saved;
    }

    private String serialize(TransactionEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize TransactionEvent for outbox", e);
        }
    }

    public Transaction updateTransaction(Long id, Transaction transactionDetails) {
        Transaction transaction = getTransactionById(id);
        transaction.setAccountId(transactionDetails.getAccountId());
        transaction.setAmount(transactionDetails.getAmount());
        transaction.setType(transactionDetails.getType());
        transaction.setDescription(transactionDetails.getDescription());
        transaction.setTransactionDate(transactionDetails.getTransactionDate());
        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        Transaction transaction = getTransactionById(id);
        transactionRepository.delete(transaction);
    }
}