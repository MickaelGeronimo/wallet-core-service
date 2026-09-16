package com.apex.wallet.infrastructure.repository;

import com.apex.wallet.domain.model.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByEmail(String email);

    Optional<Account> findByEmailIgnoreCase(String email);

    Optional<Account> findByPixKey(String pixKey);

    Optional<Account> findByPixKeyIgnoreCase(String pixKey);

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByEmail(String email);

    boolean existsByPixKey(String pixKey);

    /**
     * Acquires a pessimistic write lock (SELECT ... FOR UPDATE) on the account row.
     * Guarantees absolute sequential consistency and prevents race conditions or double spending.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);
}
