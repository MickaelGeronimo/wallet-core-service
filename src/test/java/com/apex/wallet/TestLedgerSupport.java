package com.apex.wallet;

import com.apex.wallet.domain.model.Account;
import com.apex.wallet.domain.model.LedgerEntry;
import com.apex.wallet.domain.model.LedgerType;
import com.apex.wallet.domain.model.Transaction;
import com.apex.wallet.domain.model.TransactionStatus;
import com.apex.wallet.domain.model.TransactionType;
import com.apex.wallet.infrastructure.repository.AccountRepository;
import com.apex.wallet.infrastructure.repository.LedgerEntryRepository;
import com.apex.wallet.infrastructure.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Test support that creates an account together with its opening-balance ledger entry,
 * mirroring what the application's data initializer does. Without the ledger entry, an
 * account created with a non-zero balance would break the global double-entry audit
 * (sum of balances == credits - debits) for any test that runs afterwards in the same JVM.
 */
final class TestLedgerSupport {

    private TestLedgerSupport() {
    }

    static Account saveFundedAccount(AccountRepository accountRepository,
                                     TransactionRepository transactionRepository,
                                     LedgerEntryRepository ledgerEntryRepository,
                                     Account account) {
        Account saved = accountRepository.save(account);
        BigDecimal openingBalance = saved.getBalance();
        if (openingBalance != null && openingBalance.compareTo(BigDecimal.ZERO) > 0) {
            Transaction openingTx = transactionRepository.save(new Transaction(
                    "TEST-INIT-" + UUID.randomUUID(),
                    null,
                    saved.getId(),
                    openingBalance,
                    TransactionType.DEPOSIT,
                    TransactionStatus.COMPLETED,
                    "Test opening balance"));
            ledgerEntryRepository.save(new LedgerEntry(
                    openingTx.getId(),
                    saved.getId(),
                    LedgerType.CREDIT,
                    openingBalance,
                    openingBalance,
                    "Test opening balance"));
        }
        return saved;
    }
}