package com.apex.wallet.api.controller;

import com.apex.wallet.api.dto.TransferRequest;
import com.apex.wallet.application.service.TransferService;
import com.apex.wallet.domain.model.Account;
import com.apex.wallet.domain.model.Transaction;
import com.apex.wallet.domain.model.TransactionStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transfer")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/pix")
    public ResponseEntity<Transaction> executePixTransfer(
            @AuthenticationPrincipal Account currentAccount,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        String key = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : "PIX-" + UUID.randomUUID();

        Transaction tx = transferService.executeTransfer(
                key,
                currentAccount.getId(),
                request.getTargetPixKey(),
                request.getAmount(),
                request.getDescription()
        );

        if (tx.getStatus() == TransactionStatus.QUEUED_FOR_SETTLEMENT) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(tx);
        }

        return ResponseEntity.ok(tx);
    }
}
