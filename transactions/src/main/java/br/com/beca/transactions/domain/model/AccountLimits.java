package br.com.beca.transactions.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Saldo e limites do usuário. Vêm de um serviço externo (API Mock), não do nosso banco.
 */
public record AccountLimits(
        BigDecimal balance,
        BigDecimal perTransactionLimit,
        BigDecimal dailyLimit) {

    public AccountLimits {
        Objects.requireNonNull(balance, "balance");
        Objects.requireNonNull(perTransactionLimit, "perTransactionLimit");
        Objects.requireNonNull(dailyLimit, "dailyLimit");
    }
}
