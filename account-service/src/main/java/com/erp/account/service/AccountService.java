package com.erp.account.service;

import com.erp.account.entity.Account;
import com.erp.account.repository.AccountRepository;
import com.erp.common.enums.TransactionType;
import com.erp.common.exception.BadRequestException;
import com.erp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public Page<Account> getAllAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable);
    }

    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
    }

    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    public Account updateAccount(Long id, Account accountDetails) {
        Account account = getAccountById(id);
        account.setAccountCode(accountDetails.getAccountCode());
        account.setAccountName(accountDetails.getAccountName());
        account.setAccountType(accountDetails.getAccountType());
        account.setBalance(accountDetails.getBalance());
        return accountRepository.save(account);
    }

    public void deleteAccount(Long id) {
        Account account = getAccountById(id);
        accountRepository.delete(account);
    }

    @Transactional
    public Account applyBalanceChange(Long id, TransactionType type, BigDecimal amount) {
        Account account = getAccountById(id);
        BigDecimal newBalance = type == TransactionType.CREDIT
                ? account.getBalance().add(amount)
                : account.getBalance().subtract(amount);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Insufficient balance for account id: " + id);
        }

        account.setBalance(newBalance);
        return accountRepository.save(account);
    }

}
