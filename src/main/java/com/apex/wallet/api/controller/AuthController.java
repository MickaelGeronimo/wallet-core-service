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
                "name", "Deepak Kumar",
                "role", "Senior Software Engineer @ Microsoft",
                "email", "deepak@microsoft.com",
                "password", "password123",
                "pixKey", "deepak@pix.com",
                "initialBalance", "R$ 5.000,00",
                "badge", "Engenheiro Sênior"
            ),
            Map.of(
                "name", "Marlon Bernardes",
                "role", "Software Engineer @ Microsoft (Dublin)",
                "email", "marlon@microsoft.com",
                "password", "password123",
                "pixKey", "marlon@pix.com",
                "initialBalance", "R$ 3.500,00",
                "badge", "Tech Lead"
            ),
            Map.of(
                "name", "Du Bin",
                "role", "Principal Software Engineer @ Microsoft",
                "email", "dubin@microsoft.com",
                "password", "password123",
                "pixKey", "dubin@pix.com",
                "initialBalance", "R$ 10.000,00",
                "badge", "Principal Architect"
            ),
            Map.of(
                "name", "Admin Contábil",
                "role", "Auditoria Geral & Compliance",
                "email", "admin@apex.com",
                "password", "admin123",
                "pixKey", "admin@pix.com",
                "initialBalance", "R$ 50.000,00",
                "badge", "Auditor / Admin"
            )
        );
        return ResponseEntity.ok(personas);
    }
}
