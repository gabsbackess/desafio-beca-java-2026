package br.com.beca.transactions.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExchangeRateTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);

    @Test
    void shouldConvertReaisToForeignCurrencyWithTwoDecimals() {
        ExchangeRate usd = new ExchangeRate(Currency.USD, new BigDecimal("5.00"), DAY);

        assertEquals(new BigDecimal("17.98"), usd.convert(new BigDecimal("89.90")));
    }

    @Test
    void shouldRoundHalfUp() {
        ExchangeRate eur = new ExchangeRate(Currency.EUR, new BigDecimal("3"), DAY);

        assertEquals(new BigDecimal("0.33"), eur.convert(new BigDecimal("1.00")));
        assertEquals(new BigDecimal("0.67"), eur.convert(new BigDecimal("2.00")));
    }

    @Test
    void shouldRejectZeroOrNegativeRate() {
        assertThrows(IllegalArgumentException.class, () -> new ExchangeRate(Currency.USD, BigDecimal.ZERO, DAY));
        assertThrows(IllegalArgumentException.class,
                () -> new ExchangeRate(Currency.USD, new BigDecimal("-1"), DAY));
        assertThrows(IllegalArgumentException.class, () -> new ExchangeRate(Currency.USD, null, DAY));
    }
}
