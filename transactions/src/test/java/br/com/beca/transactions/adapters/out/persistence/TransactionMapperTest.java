package br.com.beca.transactions.adapters.out.persistence;

import br.com.beca.transactions.adapters.out.persistence.entity.TransactionEntity;
import br.com.beca.transactions.adapters.out.persistence.mapper.TransactionMapper;
import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.Currency;
import br.com.beca.transactions.domain.model.ExchangeRate;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionStatus;
import br.com.beca.transactions.domain.model.TransactionType;
import br.com.beca.transactions.support.Ids;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TransactionMapperTest {

    private final TransactionMapper mapper = new TransactionMapper();
    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);

    private static Transaction transaction(List<ExchangeRate> rates) {
        return new Transaction(Ids.TX_ID, Ids.USER, null, TransactionType.PURCHASE, new BigDecimal("89.90"),
                Category.FOOD, "mercado", TransactionStatus.APPROVED, null, LocalDateTime.of(2026, 11, 10, 9, 0),
                rates);
    }

    @Test
    void shouldKeepExchangeRatesInTheRoundTrip() {
        List<ExchangeRate> rates = List.of(
                new ExchangeRate(Currency.USD, new BigDecimal("5.330000"), DAY),
                new ExchangeRate(Currency.EUR, new BigDecimal("6.150000"), DAY.minusDays(1)));

        TransactionEntity entity = mapper.toEntity(transaction(rates));
        Transaction result = mapper.toDomain(entity);

        assertEquals(new BigDecimal("5.330000"), entity.getUsdRate());
        assertEquals(DAY.minusDays(1), entity.getEurRateDate());
        assertEquals(rates, result.getExchangeRates());
        assertEquals(Category.FOOD, result.getCategory());
    }

    @Test
    void shouldMapTransactionWithoutRatesToEmptyList() {
        Transaction result = mapper.toDomain(mapper.toEntity(transaction(List.of())));

        assertFalse(result.hasExchangeRates());
    }
}
