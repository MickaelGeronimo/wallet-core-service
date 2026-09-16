package com.apex.wallet.infrastructure.config;

import com.apex.wallet.domain.model.Account;
import com.apex.wallet.domain.model.LedgerEntry;
import com.apex.wallet.domain.model.LedgerType;
import com.apex.wallet.domain.model.Transaction;
import com.apex.wallet.domain.model.TransactionStatus;
import com.apex.wallet.domain.model.TransactionType;
import com.apex.wallet.infrastructure.repository.AccountRepository;
import com.apex.wallet.infrastructure.repository.LedgerEntryRepository;
import com.apex.wallet.infrastructure.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.UUID;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.apex.wallet.application.resilience.BalanceCacheService balanceCacheService;

    public DataInitializer(AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           LedgerEntryRepository ledgerEntryRepository,
                           PasswordEncoder passwordEncoder,
                           com.apex.wallet.application.resilience.BalanceCacheService balanceCacheService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.passwordEncoder = passwordEncoder;
        this.balanceCacheService = balanceCacheService;
    }

    @Override
    public void run(String... args) {
        if (accountRepository.count() == 0) {
            log.info("Inicializando personas e contas demo com Ledger Contábil...");

            seedAccount("ACC-1001", "Lucas Silva", "lucas@wallet.local", "password123", "lucas@pix.com", new BigDecimal("5000.00"), "ROLE_USER");
            seedAccount("ACC-1002", "Beatriz Santos", "beatriz@wallet.local", "password123", "beatriz@pix.com", new BigDecimal("3500.00"), "ROLE_USER");
            seedAccount("ACC-1003", "Carlos Eduardo", "carlos@wallet.local", "password123", "carlos@pix.com", new BigDecimal("10000.00"), "ROLE_USER");
            seedAccount("ACC-9999", "Admin Contábil", "admin@wallet.local", "admin123", "admin@pix.com", new BigDecimal("50000.00"), "ROLE_ADMIN");

            log.info("Personas criadas com sucesso no Ledger!");
        }
    }

    private void seedAccount(String accNumber, String name, String email, String rawPass, String pixKey, BigDecimal initialBalance, String role) {
        Account account = new Account(
                accNumber,
                name,
                email,
                passwordEncoder.encode(rawPass),
                pixKey,
                initialBalance,
                role
        );
        Account savedAccount = accountRepository.save(account);
        balanceCacheService.put(savedAccount);

        // Record the initial capital credit in the Ledger
        Transaction initTx = new Transaction(
                "INIT-" + UUID.randomUUID(),
                null,
                savedAccount.getId(),
                initialBalance,
                TransactionType.DEPOSIT,
                TransactionStatus.COMPLETED,
                "Aporte de Capital Inicial"
        );
        Transaction savedTx = transactionRepository.save(initTx);

        LedgerEntry entry = new LedgerEntry(
                savedTx.getId(),
                savedAccount.getId(),
                LedgerType.CREDIT,
                initialBalance,
                initialBalance,
                "Aporte de Saldo Inicial"
        );
        ledgerEntryRepository.save(entry);
    }
}
