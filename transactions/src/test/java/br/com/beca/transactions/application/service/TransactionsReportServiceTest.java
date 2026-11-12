package br.com.beca.transactions.application.service;

import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.exception.InvalidTransactionException;
import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.Currency;
import br.com.beca.transactions.domain.model.ExchangeRate;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionsReportServiceTest {

    private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate NOV_30 = LocalDate.of(2026, 11, 30);

    @Mock
    private TransactionRepositoryPort transactionRepositoryPort;

    private TransactionsReportService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        service = new TransactionsReportService(transactionRepositoryPort, clock);
    }

    private static Transaction tx(TransactionType type, String amount, Category category, String description,
                                  TransactionStatus status, String reason, LocalDateTime when,
                                  List<ExchangeRate> rates) {
        return new Transaction(null, Ids.USER, null, type, new BigDecimal(amount), category, description,
                status, reason, when, rates);
    }

    private List<String> lines(String csv) {
        return List.of(csv.split("\r\n"));
    }

    @Test
    void shouldStartWithTheHeaderWithSemicolonSeparatorAndNoBom() {
        when(transactionRepositoryPort.findByUser(Ids.USER, NOV_1, NOV_30, null)).thenReturn(List.of());

        String csv = service.generateCsv(Ids.USER, null, null);

        assertEquals("data;tipo;categoria;descricao;valor_brl;status;motivo;valor_usd;valor_eur\r\n", csv);
        assertFalse(csv.startsWith("﻿"));
    }

    @Test
    void shouldWriteApprovedRowWithDecimalCommaAndConvertedValues() {
        List<ExchangeRate> rates = List.of(
                new ExchangeRate(Currency.USD, new BigDecimal("5.00"), NOV_1),
                new ExchangeRate(Currency.EUR, new BigDecimal("6.00"), NOV_1));
        when(transactionRepositoryPort.findByUser(Ids.USER, NOV_1, NOV_30, null)).thenReturn(List.of(
                tx(TransactionType.PURCHASE, "89.90", Category.FOOD, "Mercado", TransactionStatus.APPROVED, null,
                        LocalDateTime.of(2026, 11, 5, 14, 30), rates)));

        List<String> lines = lines(service.generateCsv(Ids.USER, null, null));

        assertEquals("05/11/2026 14:30;Compra;Alimentação;Mercado;89,90;Aprovada;;17,98;14,98", lines.get(1));
    }

    @Test
    void shouldWriteRejectedRowWithReasonAndEmptyConversionWhenThereAreNoRates() {
        when(transactionRepositoryPort.findByUser(Ids.USER, NOV_1, NOV_30, null)).thenReturn(List.of(
                tx(TransactionType.WITHDRAWAL, "5000.00", Category.OTHER, "", TransactionStatus.REJECTED,
                        "Saldo insuficiente.", LocalDateTime.of(2026, 11, 6, 9, 0), List.of())));

        List<String> lines = lines(service.generateCsv(Ids.USER, null, null));

        assertEquals("06/11/2026 09:00;Retirada;Outros;;5000,00;Rejeitada;Saldo insuficiente.;;", lines.get(1));
    }

    @Test
    void shouldListTransactionsFromTheOldestToTheNewest() {
        when(transactionRepositoryPort.findByUser(Ids.USER, NOV_1, NOV_30, null)).thenReturn(List.of(
                tx(TransactionType.DEPOSIT, "10.00", Category.OTHER, "novo", TransactionStatus.APPROVED, null,
                        LocalDateTime.of(2026, 11, 9, 9, 0), List.of()),
                tx(TransactionType.DEPOSIT, "10.00", Category.OTHER, "antigo", TransactionStatus.APPROVED, null,
                        LocalDateTime.of(2026, 11, 2, 9, 0), List.of())));

        List<String> lines = lines(service.generateCsv(Ids.USER, null, null));

        assertTrue(lines.get(1).contains("antigo"));
        assertTrue(lines.get(2).contains("novo"));
    }

    @Test
    void shouldQuoteTextWithSemicolonAndDoubleTheQuotes() {
        assertEquals("\"almoço; com \"\"amigos\"\"\"", TransactionsReportService.text("almoço; com \"amigos\""));
    }

    @Test
    void shouldNeutralizeFormulasThatTheUserTypedInTheDescription() {
        assertEquals("'=SOMA(A1)", TransactionsReportService.text("=SOMA(A1)"));
        assertEquals("'@usuario", TransactionsReportService.text("@usuario"));
        assertEquals("'-1+1", TransactionsReportService.text("-1+1"));
    }

    @Test
    void shouldKeepNormalTextAndEmptyValues() {
        assertEquals("Mercado do mês", TransactionsReportService.text("Mercado do mês"));
        assertEquals("", TransactionsReportService.text(null));
        assertEquals("", TransactionsReportService.text(""));
    }

    @Test
    void shouldUseDecimalComma() {
        assertEquals("1234,50", TransactionsReportService.money(new BigDecimal("1234.50")));
    }

    @Test
    void shouldAskTheRepositoryOnlyForTheGivenUserAndPeriod() {
        when(transactionRepositoryPort.findByUser(Ids.USER, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31), null)).thenReturn(List.of());

        service.generateCsv(Ids.USER, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        verify(transactionRepositoryPort).findByUser(Ids.USER, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31), null);
    }

    @Test
    void shouldRejectInvertedPeriod() {
        assertThrows(InvalidTransactionException.class,
                () -> service.generateCsv(Ids.USER, NOV_30, NOV_1));
    }
}
