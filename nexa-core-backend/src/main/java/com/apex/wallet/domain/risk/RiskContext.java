package com.apex.wallet.domain.risk;

import com.apex.wallet.domain.model.Account;

import java.math.BigDecimal;

public record RiskContext(
        Account senderAccount,
        String targetPixKey,
        BigDecimal amount,
        String clientIp,
        long recentTransactionsInLastMinute,
        BigDecimal totalSpentInLastHour
) {}
