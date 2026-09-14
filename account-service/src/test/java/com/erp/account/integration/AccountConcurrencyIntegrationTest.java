package com.erp.account.integration;

import com.erp.account.entity.Account;
import com.erp.account.entity.AccountType;
import com.erp.account.repository.AccountRepository;
import com.erp.account.service.AccountService;
import com.erp.common.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The most important test in this suite: proves that the @Version field on Account
 * actually prevents the classic "lost update" race condition under genuine concurrent
 * writes, rather than merely existing on the entity without doing anything.
 *
 * Ten threads each try to CREDIT the SAME account by +10 at (as close to) the same
 * moment, via the real AccountService.applyBalanceChange method - the same method the
 * HTTP endpoint delegates to. Without optimistic locking, two threads reading the same
 * "current balance" and both writing back "current + 10" would silently lose one of
 * the increments. With @Version, a losing writer's save throws
 * ObjectOptimisticLockingFailureException instead of silently overwriting - so each
 * thread retries (re-reading the latest balance) until its own write succeeds. If (and
 * only if) optimistic locking is working correctly, retrying like this guarantees
 * every one of the 10 increments is eventually applied with none lost, and the final
 * balance equals starting balance + (10 * numberOfThreads).
 */
class AccountConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void concurrentBalanceAdjustments_loseNoUpdates() throws Exception {
        Account account = new Account();
        account.setAccountCode("CONC-" + System.nanoTime());
        account.setAccountName("Concurrency Test Account");
        account.setAccountType(AccountType.ASSET);
        account.setBalance(new BigDecimal("1000.00"));
        account = accountRepository.saveAndFlush(account);
        Long accountId = account.getId();

        int threadCount = 10;
        BigDecimal adjustmentPerThread = new BigDecimal("10.00");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger totalRetries = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Long id = accountId;
            tasks.add(() -> {
                readyLatch.countDown();
                startLatch.await();

                // Retry-on-conflict: this is the correct way to use optimistic locking,
                // not a workaround for a bug. Each retry re-fetches the current balance
                // inside applyBalanceChange, so no attempt is ever lost - it just may
                // need to be re-applied on top of a newer version.
                int attempts = 0;
                while (true) {
                    attempts++;
                    try {
                        accountService.applyBalanceChange(id, TransactionType.CREDIT, adjustmentPerThread);
                        break;
                    } catch (ObjectOptimisticLockingFailureException e) {
                        totalRetries.incrementAndGet();
                        if (attempts > 100) {
                            throw e;
                        }
                    }
                }
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(executor.submit(task));
        }
        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();
        for (Future<Void> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        Account finalAccount = accountRepository.findById(accountId).orElseThrow();
        BigDecimal expectedBalance = new BigDecimal("1000.00")
                .add(adjustmentPerThread.multiply(BigDecimal.valueOf(threadCount)));

        assertThat(finalAccount.getBalance()).isEqualByComparingTo(expectedBalance);
        // version should have advanced by exactly one per successful write - proof that
        // every one of the 10 concurrent writers actually got a turn (no lost updates
        // silently overwriting each other without bumping the version).
        assertThat(finalAccount.getVersion()).isEqualTo((long) threadCount);
    }
}
