package com.erp.transaction.integration;

import com.erp.common.exception.BadRequestException;
import com.erp.common.exception.ResourceNotFoundException;
import com.erp.transaction.client.AccountLookupService;
import com.erp.transaction.dto.AccountResponse;
import com.erp.transaction.repository.OutboxEventRepository;
import com.erp.transaction.repository.TransactionRepository;
import com.erp.transaction.support.PostgresTestContainer;
import com.erp.transaction.support.TestJwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack tests with a real Postgres (Testcontainers) but a mocked
 * AccountLookupService, so we never depend on a live account-service.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class TransactionControllerIntegrationTest extends PostgresTestContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockBean
    private AccountLookupService accountLookupService;

    @AfterEach
    void resetMock() {
        reset(accountLookupService);
    }

    private String txnJson(long accountId, String amount, String type) {
        return String.format("{\"accountId\":%d,\"amount\":%s,\"type\":\"%s\",\"description\":\"test\"}",
                accountId, amount, type);
    }

    @Test
    void createTransaction_persistsTransactionAndOutboxRow_whenAccountLookupSucceeds() throws Exception {
        String userAuth = "Bearer " + TestJwtUtil.userToken();
        AccountResponse account = new AccountResponse();
        account.setId(100L);
        when(accountLookupService.getAccount(100L)).thenReturn(account);
        when(accountLookupService.adjustBalance(eq(100L), any(), any())).thenReturn(account);

        long outboxCountBefore = outboxEventRepository.count();

        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txnJson(100L, "25.00", "DEBIT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(100))
                .andReturn();

        Long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString()).get("id").asLong();

        assertThat(transactionRepository.findById(id)).isPresent();
        assertThat(outboxEventRepository.count()).isEqualTo(outboxCountBefore + 1);
    }

    @Test
    void createTransaction_returns404_andWritesNothing_whenAccountNotFound() throws Exception {
        String userAuth = "Bearer " + TestJwtUtil.userToken();
        when(accountLookupService.getAccount(anyLong()))
                .thenThrow(new ResourceNotFoundException("Account not found with id: 555"));

        long txnCountBefore = transactionRepository.count();
        long outboxCountBefore = outboxEventRepository.count();

        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txnJson(555L, "10.00", "CREDIT")))
                .andExpect(status().isNotFound());

        assertThat(transactionRepository.count()).isEqualTo(txnCountBefore);
        assertThat(outboxEventRepository.count()).isEqualTo(outboxCountBefore);
    }

    /**
     * Regression test for the @Transactional rollback behavior documented in
     * TransactionService.createTransaction: if adjustBalance fails AFTER the
     * transaction row has already been saved (but before commit), the whole
     * method's transaction rolls back and no orphaned transaction row is left
     * in the database - even though transactionRepository.save() ran and
     * returned successfully earlier in the same method.
     */
    @Test
    void createTransaction_rollsBackTransactionRow_whenAdjustBalanceFailsAfterSave() throws Exception {
        String userAuth = "Bearer " + TestJwtUtil.userToken();
        AccountResponse account = new AccountResponse();
        account.setId(200L);
        when(accountLookupService.getAccount(200L)).thenReturn(account);
        when(accountLookupService.adjustBalance(eq(200L), any(), any()))
                .thenThrow(new BadRequestException("Unable to adjust balance - insufficient funds"));

        long txnCountBefore = transactionRepository.count();
        long outboxCountBefore = outboxEventRepository.count();

        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txnJson(200L, "1000000.00", "DEBIT")))
                .andExpect(status().isBadRequest());

        // No orphaned transaction row: the earlier transactionRepository.save() call
        // inside the @Transactional method must have been rolled back.
        assertThat(transactionRepository.count()).isEqualTo(txnCountBefore);
        assertThat(transactionRepository.findAll().stream()
                .noneMatch(t -> t.getAccountId().equals(200L))).isTrue();
        assertThat(outboxEventRepository.count()).isEqualTo(outboxCountBefore);
    }

    @Test
    void deleteTransaction_requiresAdminRole() throws Exception {
        String userAuth = "Bearer " + TestJwtUtil.userToken();
        String adminAuth = "Bearer " + TestJwtUtil.adminToken();

        AccountResponse account = new AccountResponse();
        account.setId(300L);
        when(accountLookupService.getAccount(300L)).thenReturn(account);
        when(accountLookupService.adjustBalance(eq(300L), any(), any())).thenReturn(account);

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txnJson(300L, "5.00", "CREDIT")))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/transactions/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, userAuth))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/transactions/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isNoContent());

        assertThat(transactionRepository.findById(id)).isEmpty();
    }
}
