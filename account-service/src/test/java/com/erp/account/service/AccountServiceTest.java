package com.erp.account.service;

import com.erp.account.entity.Account;
import com.erp.account.entity.AccountType;
import com.erp.account.repository.AccountRepository;
import com.erp.common.enums.TransactionType;
import com.erp.common.exception.BadRequestException;
import com.erp.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository);
    }

    private Account accountWithBalance(BigDecimal balance) {
        Account account = new Account();
        account.setId(1L);
        account.setAccountCode("ACC-001");
        account.setAccountName("Cash");
        account.setAccountType(AccountType.ASSET);
        account.setBalance(balance);
        return account;
    }

    @Test
    void applyBalanceChange_creditIncreasesBalance() {
        Account account = accountWithBalance(new BigDecimal("100.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.applyBalanceChange(1L, TransactionType.CREDIT, new BigDecimal("50.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("150.00");
    }

    @Test
    void applyBalanceChange_debitDecreasesBalance() {
        Account account = accountWithBalance(new BigDecimal("100.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.applyBalanceChange(1L, TransactionType.DEBIT, new BigDecimal("30.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("70.00");
    }

    /**
     * Explicit regression test proving CREDIT and DEBIT are not swapped. This logic
     * was previously a fragile .equalsIgnoreCase() string comparison and was refactored
     * to a strict enum equality check; a silent direction inversion here would be a
     * serious financial correctness bug (debits would increase balances and vice
     * versa) that a superficial "balance changed" assertion would not catch.
     */
    @Test
    void applyBalanceChange_creditAndDebitAreNotInverted() {
        Account creditAccount = accountWithBalance(new BigDecimal("100.00"));
        Account debitAccount = accountWithBalance(new BigDecimal("100.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(creditAccount), Optional.of(debitAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account afterCredit = accountService.applyBalanceChange(1L, TransactionType.CREDIT, new BigDecimal("25.00"));
        Account afterDebit = accountService.applyBalanceChange(1L, TransactionType.DEBIT, new BigDecimal("25.00"));

        assertThat(afterCredit.getBalance()).isGreaterThan(new BigDecimal("100.00"));
        assertThat(afterDebit.getBalance()).isLessThan(new BigDecimal("100.00"));
        assertThat(afterCredit.getBalance()).isEqualByComparingTo("125.00");
        assertThat(afterDebit.getBalance()).isEqualByComparingTo("75.00");
    }

    @Test
    void applyBalanceChange_debitThatWouldGoNegative_throwsAndDoesNotSave() {
        Account account = accountWithBalance(new BigDecimal("10.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.applyBalanceChange(1L, TransactionType.DEBIT, new BigDecimal("50.00")))
                .isInstanceOf(BadRequestException.class);

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void applyBalanceChange_debitToExactlyZero_isAllowed() {
        Account account = accountWithBalance(new BigDecimal("50.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.applyBalanceChange(1L, TransactionType.DEBIT, new BigDecimal("50.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void applyBalanceChange_accountNotFound_throwsResourceNotFoundException() {
        when(accountRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.applyBalanceChange(404L, TransactionType.CREDIT, BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllAccounts_delegatesToRepository() {
        Pageable pageable = mock(Pageable.class);
        Page<Account> page = new PageImpl<>(List.of(accountWithBalance(BigDecimal.ZERO)));
        when(accountRepository.findAll(pageable)).thenReturn(page);

        Page<Account> result = accountService.getAllAccounts(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getAccountById_returnsAccount_whenFound() {
        Account account = accountWithBalance(BigDecimal.ZERO);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThat(accountService.getAccountById(1L)).isEqualTo(account);
    }

    @Test
    void getAccountById_throwsResourceNotFoundException_whenMissing() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createAccount_savesAndReturnsAccount() {
        Account account = accountWithBalance(BigDecimal.ZERO);
        when(accountRepository.save(account)).thenReturn(account);

        Account result = accountService.createAccount(account);

        assertThat(result).isEqualTo(account);
        verify(accountRepository).save(account);
    }

    @Test
    void updateAccount_updatesFieldsAndSaves() {
        Account existing = accountWithBalance(new BigDecimal("10.00"));
        Account updateRequest = new Account();
        updateRequest.setAccountCode("ACC-002");
        updateRequest.setAccountName("Updated Cash");
        updateRequest.setAccountType(AccountType.LIABILITY);
        updateRequest.setBalance(new BigDecimal("999.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.updateAccount(1L, updateRequest);

        assertThat(result.getAccountCode()).isEqualTo("ACC-002");
        assertThat(result.getAccountName()).isEqualTo("Updated Cash");
        assertThat(result.getAccountType()).isEqualTo(AccountType.LIABILITY);
        assertThat(result.getBalance()).isEqualByComparingTo("999.00");
    }

    @Test
    void updateAccount_throwsResourceNotFoundException_whenMissing() {
        when(accountRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.updateAccount(404L, new Account()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteAccount_deletesAccount_whenFound() {
        Account account = accountWithBalance(BigDecimal.ZERO);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        accountService.deleteAccount(1L);

        verify(accountRepository, times(1)).delete(account);
    }

    @Test
    void deleteAccount_throwsResourceNotFoundException_whenMissing() {
        when(accountRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.deleteAccount(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
