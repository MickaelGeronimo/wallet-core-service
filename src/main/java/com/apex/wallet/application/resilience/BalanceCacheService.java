package com.apex.wallet.application.resilience;

import com.apex.wallet.domain.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-speed In-Memory Balance Cache for Graceful Degradation.
 * Serves customer balance lookups in < 0.2ms if the primary relational database
 * is suffering from connection pool exhaustion or latency spikes.
 */
@Service
public class BalanceCacheService {

    private static final Logger log = LoggerFactory.getLogger(BalanceCacheService.class);

    private final Map<Long, CachedBalance> accountBalances = new ConcurrentHashMap<>();
    private final Map<String, Long> pixKeyToAccountId = new ConcurrentHashMap<>();

    public record CachedBalance(BigDecimal balance, String holderName, String accountNumber, Instant lastUpdated) {}

    public void put(Account account) {
        if (account != null && account.getId() != null) {
            accountBalances.put(account.getId(), new CachedBalance(
                    account.getBalance(),
                    account.getHolderName(),
                    account.getAccountNumber(),
                    Instant.now()
            ));
            if (account.getPixKey() != null) {
                pixKeyToAccountId.put(account.getPixKey().toLowerCase(), account.getId());
            }
        }
    }

    public void updateBalance(Long accountId, BigDecimal newBalance) {
        accountBalances.computeIfPresent(accountId, (id, current) ->
                new CachedBalance(newBalance, current.holderName(), current.accountNumber(), Instant.now()));
    }

    public Optional<CachedBalance> getBalance(Long accountId) {
        return Optional.ofNullable(accountBalances.get(accountId));
    }

    public Optional<Long> findAccountIdByPixKey(String pixKey) {
        if (pixKey == null) return Optional.empty();
        return Optional.ofNullable(pixKeyToAccountId.get(pixKey.toLowerCase()));
    }

    public int getCachedAccountCount() {
        return accountBalances.size();
    }
}
