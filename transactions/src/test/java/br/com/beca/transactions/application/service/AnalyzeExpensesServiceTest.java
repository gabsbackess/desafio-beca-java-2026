package br.com.beca.transactions.application.service;

import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.ExpenseSummary;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionStatus;
import br.com.beca.transactions.domain.model.TransactionType;
import br.com.beca.transactions.support.Ids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyzeExpensesServiceTest {

    @Mock
    private TransactionRepositoryPort transactionRepositoryPort;

    private AnalyzeExpensesService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        service = new AnalyzeExpensesService(transactionRepositoryPort, clock);
    }

    @Test
    void shouldAnalyzeTheCurrentMonthOfTheUserWhenNoPeriodIsGiven() {
        Transaction purchase = new Transaction(null, Ids.USER, null, TransactionType.PURCHASE,
                new BigDecimal("80.00"), Category.FOOD, "", TransactionStatus.APPROVED, null,
                LocalDateTime.of(2026, 11, 3, 10, 0));
        when(transactionRepositoryPort.findByUser(Ids.USER, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), null)).thenReturn(List.of(purchase));

        ExpenseSummary summary = service.analyze(Ids.USER, null, null);

        assertEquals(LocalDate.of(2026, 11, 1), summary.from());
        assertEquals(LocalDate.of(2026, 11, 30), summary.to());
        assertEquals(new BigDecimal("80.00"), summary.total());
        verify(transactionRepositoryPort).findByUser(Ids.USER, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), null);
    }
}
