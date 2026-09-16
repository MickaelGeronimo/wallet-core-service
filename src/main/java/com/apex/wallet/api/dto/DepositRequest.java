package com.apex.wallet.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class DepositRequest {

    @NotNull(message = "O valor é obrigatório")
    @DecimalMin(value = "1.00", message = "O valor mínimo de depósito é R$ 1,00")
    @Digits(integer = 15, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais")
    private BigDecimal amount;

    private String description;

    public DepositRequest() {}

    public DepositRequest(BigDecimal amount, String description) {
        this.amount = amount;
        this.description = description;
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
