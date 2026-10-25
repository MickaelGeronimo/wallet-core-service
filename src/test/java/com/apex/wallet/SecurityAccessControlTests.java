package com.apex.wallet;

import com.apex.wallet.application.service.TransferService;
import com.apex.wallet.domain.model.Account;
import com.apex.wallet.domain.model.Transaction;
import com.apex.wallet.infrastructure.repository.AccountRepository;
import com.apex.wallet.infrastructure.security.JwtService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression tests for access control and idempotency-key ownership.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessControlTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferService transferService;

    @Autowired
    private com.apex.wallet.infrastructure.repository.TransactionRepository transactionRepository;

    @Autowired
    private com.apex.wallet.infrastructure.repository.LedgerEntryRepository ledgerEntryRepository;

    private String bearerFor(String email) {
        Account acc = accountRepository.findByEmail(email).orElseThrow();
        return "Bearer " + jwtService.generateToken(acc.getEmail(), acc.getId(), acc.getHolderName(), acc.getRole(), acc.getPixKey());
    }

    @Test
    @DisplayName("Audit endpoints: anonymous -> 401, regular user -> 403, admin -> 200")
    void auditEndpointsRequireAdmin() throws Exception {
        mockMvc.perform(get("/api/audit/all-transactions"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/audit/all-transactions").header("Authorization", bearerFor("lucas@wallet.local")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/audit/all-transactions").header("Authorization", bearerFor("admin@wallet.local")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Chaos endpoints: anonymous and regular users cannot inject a database outage")
    void chaosEndpointsRequireAdmin() throws Exception {
        mockMvc.perform(post("/api/chaos/force-failure").param("fail", "true"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/chaos/force-failure").param("fail", "true")
                        .header("Authorization", bearerFor("lucas@wallet.local")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/chaos/status").header("Authorization", bearerFor("admin@wallet.local")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forceDatabaseFailure").value(false));
    }

    @Test
    @DisplayName("Actuator: health stays public, metrics require admin")
    void actuatorExposure() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unknown URL returns 404, not 500")
    void unknownRouteReturns404() throws Exception {
        mockMvc.perform(get("/api/rota-que-nao-existe").header("Authorization", bearerFor("lucas@wallet.local")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Idempotency: another account reusing an existing key must NOT receive the original transaction")
    void idempotencyKeyCannotLeakAcrossAccounts() {
        // Fresh accounts: the shared demo personas may already be near the velocity-burst limit from other tests.
        Account owner = newAccount("Owner");
        Account intruder = newAccount("Intruder");
        Account recipient = newAccount("Recipient");

        String key = "OWNERSHIP-" + UUID.randomUUID();
        Transaction original = transferService.executeTransfer(key, owner.getId(), recipient.getPixKey(), new BigDecimal("5.00"), "Original");
        Assertions.assertNotNull(original.getId());

        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class,
                () -> transferService.executeTransfer(key, intruder.getId(), recipient.getPixKey(), new BigDecimal("5.00"), "Replay alheio"));
        Assertions.assertTrue(ex.getMessage().contains("idempotência"));

        // A deposit cannot hijack a transfer key either
        Assertions.assertThrows(IllegalStateException.class,
                () -> transferService.executeDeposit(key, owner.getId(), new BigDecimal("1.00"), "Deposito com chave de PIX"));

        // The legitimate owner still gets the idempotent replay
        Transaction replay = transferService.executeTransfer(key, owner.getId(), recipient.getPixKey(), new BigDecimal("5.00"), "Retry");
        Assertions.assertEquals(original.getId(), replay.getId());
    }

    private Account newAccount(String label) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return TestLedgerSupport.saveFundedAccount(accountRepository, transactionRepository, ledgerEntryRepository, new Account(
                "IDEM-" + suffix,
                label + " " + suffix,
                "idem" + suffix + "@wallet.local",
                "secret",
                "idem" + suffix + "@pix.com",
                new BigDecimal("1000.00"),
                "ROLE_USER"));
    }
}
