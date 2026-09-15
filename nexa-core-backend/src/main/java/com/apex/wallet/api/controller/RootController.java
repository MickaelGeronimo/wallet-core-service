package com.apex.wallet.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> index() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("system", "NEXA Core Platform — Banking Ledger Engine");
        resp.put("version", "1.0.0");
        resp.put("status", "UP");
        resp.put("timestamp", Instant.now().toString());
        resp.put("architecture", "Double-Entry Bookkeeping Ledger (ACID Compliant, Pessimistic Locking)");
        resp.put("frontendUrl", "http://localhost:3000");
        resp.put("swaggerUi", "http://localhost:8080/swagger-ui/index.html");
        resp.put("h2Console", "http://localhost:8080/h2-console");

        Map<String, String> publicEndpoints = new LinkedHashMap<>();
        publicEndpoints.put("POST /api/auth/login", "Autenticação via e-mail ou conta demo");
        publicEndpoints.put("GET /api/auth/personas", "Contas de teste pré-carregadas");
        publicEndpoints.put("GET /api/audit/balance", "Auditoria de integridade matemática em tempo real");
        publicEndpoints.put("GET /api/audit/entries", "Extrato global imutável das partidas dobradas");
        publicEndpoints.put("GET /api/wallet/pix-lookup?key={chave}", "Consulta de chave PIX");
        resp.put("publicEndpoints", publicEndpoints);

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "engine", "NEXA Double-Entry Ledger",
                "concurrency", "PESSIMISTIC_WRITE"
        ));
    }
}
