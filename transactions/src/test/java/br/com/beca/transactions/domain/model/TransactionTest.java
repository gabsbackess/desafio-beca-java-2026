package br.com.beca.transactions.domain.model;

import br.com.beca.transactions.support.Ids;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 9, 0);

    private static TransactionRequest purchase() {
        return new TransactionRequest(Ids.USER, TransactionType.PURCHASE, new BigDecimal("50"),
                Category.FOOD, "mercado", null);
    }

    @Test
    void shouldBuildApprovedTransactionWithoutIdFromRequestAndDecision() {
        Transaction transaction = Transaction.from(purchase(), ApprovalDecision.approved(), NOW);

        assertNull(transaction.getId());
        assertEquals(Ids.USER, transaction.getUserId());
        assertEquals(new BigDecimal("50.00"), transaction.getAmount());
        assertEquals(Category.FOOD, transaction.getCategory());
        assertTrue(transaction.isApproved());
        assertNull(transaction.getRejectionReason());
        assertEquals(NOW, transaction.getCreatedAt());
        assertFalse(transaction.hasExchangeRates());
    }

    @Test
    void shouldKeepTheReasonWhenRejected() {
        Transaction transaction = Transaction.from(purchase(), ApprovalDecision.rejected("Saldo insuficiente."), NOW);

        assertFalse(transaction.isApproved());
        assertEquals(TransactionStatus.REJECTED, transaction.getStatus());
        assertEquals("Saldo insuficiente.", transaction.getRejectionReason());
    }

    @Test
    void shouldReturnACopyWithTheRatesAndKeepTheOriginalUntouched() {
        Transaction original = Transaction.from(purchase(), ApprovalDecision.approved(), NOW);
        List<ExchangeRate> rates = List.of(
                new ExchangeRate(Currency.USD, new BigDecimal("5.30"), LocalDate.of(2026, 11, 10)));

        Transaction copy = original.withExchangeRates(rates);

        assertTrue(copy.hasExchangeRates());
        assertEquals(rates, copy.getExchangeRates());
        assertFalse(original.hasExchangeRates());
        assertEquals(original.getAmount(), copy.getAmount());
        assertEquals(original.getCreatedAt(), copy.getCreatedAt());
    }

    @Test
    void shouldNotAllowChangingTheListOfRates() {
        Transaction transaction = Transaction.from(purchase(), ApprovalDecision.approved(), NOW)
                .withExchangeRates(List.of(new ExchangeRate(Currency.EUR, BigDecimal.ONE, LocalDate.of(2026, 11, 10))));

        assertThrows(UnsupportedOperationException.class, () -> transaction.getExchangeRates().clear());
    }
}
