package br.com.beca.transactions.application.service;

import br.com.beca.transactions.domain.exception.InvalidTransactionException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportPeriodTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"),
            ZoneId.of("America/Sao_Paulo"));

    @Test
    void shouldUseTheCurrentMonthWhenNoDatesAreGiven() {
        ReportPeriod period = ReportPeriod.resolve(null, null, CLOCK);

        assertEquals(LocalDate.of(2026, 11, 1), period.from());
        assertEquals(LocalDate.of(2026, 11, 30), period.to());
    }

    @Test
    void shouldStartAtTheFirstDayOfTheMonthWhenOnlyTheEndIsGiven() {
        ReportPeriod period = ReportPeriod.resolve(null, LocalDate.of(2026, 10, 15), CLOCK);

        assertEquals(LocalDate.of(2026, 10, 1), period.from());
        assertEquals(LocalDate.of(2026, 10, 15), period.to());
    }

    @Test
    void shouldEndAtTheLastDayOfTheMonthWhenOnlyTheStartIsGiven() {
        ReportPeriod period = ReportPeriod.resolve(LocalDate.of(2026, 2, 10), null, CLOCK);

        assertEquals(LocalDate.of(2026, 2, 28), period.to());
    }

    @Test
    void shouldKeepBothDatesWhenGiven() {
        ReportPeriod period = ReportPeriod.resolve(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 3, 9), CLOCK);

        assertEquals(LocalDate.of(2026, 1, 5), period.from());
        assertEquals(LocalDate.of(2026, 3, 9), period.to());
    }

    @Test
    void shouldRejectStartAfterEnd() {
        assertThrows(InvalidTransactionException.class,
                () -> ReportPeriod.resolve(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 1), CLOCK));
    }
}
