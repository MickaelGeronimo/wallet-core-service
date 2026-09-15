package com.apex.wallet.application.service;

import com.apex.wallet.domain.model.Account;
import com.apex.wallet.infrastructure.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada para o ID: " + id));
    }

    @Transactional(readOnly = true)
    public Account getAccountByEmail(String email) {
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada para o e-mail: " + email));
    }

    @Transactional(readOnly = true)
    public Optional<Account> lookupPixKey(String pixKey) {
        return accountRepository.findByPixKey(pixKey);
    }

    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }
}
