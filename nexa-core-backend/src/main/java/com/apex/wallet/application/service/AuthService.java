package com.apex.wallet.application.service;

import com.apex.wallet.domain.model.Account;
import com.apex.wallet.infrastructure.repository.AccountRepository;
import com.apex.wallet.infrastructure.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AccountRepository accountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Map<String, Object> authenticate(String email, String rawPassword) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas. Conta não encontrada."));

        if (!passwordEncoder.matches(rawPassword, account.getPassword())) {
            throw new IllegalArgumentException("Credenciais inválidas. Senha incorreta.");
        }

        String token = jwtService.generateToken(account.getEmail(), account.getId(), account.getHolderName(), account.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("accountId", account.getId());
        response.put("accountNumber", account.getAccountNumber());
        response.put("holderName", account.getHolderName());
        response.put("email", account.getEmail());
        response.put("pixKey", account.getPixKey());
        response.put("balance", account.getBalance());
        response.put("role", account.getRole());

        return response;
    }
}
