package com.apex.wallet.api.controller;

import com.apex.wallet.api.dto.LoginRequest;
import com.apex.wallet.application.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> response = authService.authenticate(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/personas")
    public ResponseEntity<List<Map<String, String>>> getDemoPersonas() {
        List<Map<String, String>> personas = Arrays.asList(
            Map.of(
                "name", "Lucas Silva",
                "role", "Correntista Individual",
                "email", "lucas@wallet.local",
                "password", "password123",
                "pixKey", "lucas@pix.com",
                "initialBalance", "R$ 5.000,00",
                "badge", "Usuário Demo 1"
            ),
            Map.of(
                "name", "Beatriz Santos",
                "role", "Correntista Individual",
                "email", "beatriz@wallet.local",
                "password", "password123",
                "pixKey", "beatriz@pix.com",
                "initialBalance", "R$ 3.500,00",
                "badge", "Usuário Demo 2"
            ),
            Map.of(
                "name", "Carlos Eduardo",
                "role", "Pessoa Jurídica / Lojista",
                "email", "carlos@wallet.local",
                "password", "password123",
                "pixKey", "carlos@pix.com",
                "initialBalance", "R$ 10.000,00",
                "badge", "Usuário Demo 3"
            ),
            Map.of(
                "name", "Admin Contábil",
                "role", "Auditoria Geral & Compliance",
                "email", "admin@wallet.local",
                "password", "admin123",
                "pixKey", "admin@pix.com",
                "initialBalance", "R$ 50.000,00",
                "badge", "Auditor / Admin"
            )
        );
        return ResponseEntity.ok(personas);
    }
}
