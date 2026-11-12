package br.com.beca.transactions.application.service;

import br.com.beca.transactions.domain.exception.InvalidTransactionException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Período de uma análise ou de um relatório (datas inclusivas).
 * Sem datas: o mês atual. Só a data final: do dia 1º daquele mês até ela.
 * Só a data inicial: até o último dia daquele mês.
 */
public record ReportPeriod(LocalDate from, LocalDate to) {

    public static ReportPeriod resolve(LocalDate from, LocalDate to, Clock clock) {
        LocalDate start = from;
        LocalDate end = to;
        if (start == null && end == null) {
            LocalDate today = LocalDate.now(clock);
            start = today.withDayOfMonth(1);
            end = today.with(TemporalAdjusters.lastDayOfMonth());
        } else if (start == null) {
            start = end.withDayOfMonth(1);
        } else if (end == null) {
            end = start.with(TemporalAdjusters.lastDayOfMonth());
        }
        if (start.isAfter(end)) {
            throw new InvalidTransactionException("A data inicial não pode ser depois da data final.");
        }
        return new ReportPeriod(start, end);
    }
}
