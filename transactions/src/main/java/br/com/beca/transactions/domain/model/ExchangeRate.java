package br.com.beca.transactions.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Taxa de câmbio: quantos reais vale uma unidade da moeda, e a data da cotação.
 */
public record ExchangeRate(Currency currency, BigDecimal rate, LocalDate date) {

    public ExchangeRate {
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(date, "date");
        if (rate == null || rate.signum() <= 0) {
            throw new IllegalArgumentException("A taxa de câmbio deve ser maior que zero.");
        }
    }

    /** Converte um valor em reais para a moeda desta taxa, com 2 casas decimais. */
    public BigDecimal convert(BigDecimal amountInBrl) {
        return amountInBrl.divide(rate, 2, RoundingMode.HALF_UP);
    }
}
