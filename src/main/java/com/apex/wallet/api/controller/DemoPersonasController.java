package com.apex.wallet.api.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Exposes the demo personas (including their plaintext demo passwords) for local exploration.
 * Mirrors {@link com.apex.wallet.infrastructure.config.DataInitializer}: never registered under the "prod" profile,
 * so the admin credentials are not published in a production deployment.
 */
@RestController
@RequestMapping("/api/auth")
@Profile("!prod")
public class DemoPersonasController {

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
