package com.apex.wallet.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TransferRequest {

    @NotBlank(message = "A chave PIX do destinatário é obrigatória")
    private String targetPixKey;

    @NotNull(message = "O valor é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor mínimo de transferência é R$ 0,01")
    @DecimalMax(value = "1000000.00", message = "O valor máximo de transferência é R$ 1.000.000,00")
    @Digits(integer = 15, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais")
    private BigDecimal amount;

    private String description;

    public TransferRequest() {}

    public TransferRequest(String targetPixKey, BigDecimal amount, String description) {
        this.targetPixKey = targetPixKey;
        this.amount = amount;
        this.description = description;
    }

    public String getTargetPixKey() { return targetPixKey; }
    public void setTargetPixKey(String targetPixKey) { this.targetPixKey = targetPixKey; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
