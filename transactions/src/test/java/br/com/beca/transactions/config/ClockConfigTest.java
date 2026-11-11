package br.com.beca.transactions.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClockConfigTest {

    @Test
    void shouldUseTheConfiguredTimezone() {
        Clock clock = new ClockConfig().clock("America/Sao_Paulo");

        assertEquals(ZoneId.of("America/Sao_Paulo"), clock.getZone());
    }

    @Test
    void shouldCountTheDayInSaoPauloAndNotInUtc() {
        Clock clock = new ClockConfig().clock("America/Sao_Paulo");
        // 02:30 UTC do dia 11 ainda é 23:30 do dia 10 em São Paulo.
        Instant instant = Instant.parse("2026-11-11T02:30:00Z");

        assertEquals(LocalDate.of(2026, 11, 10), LocalDate.ofInstant(instant, clock.getZone()));
    }
}
