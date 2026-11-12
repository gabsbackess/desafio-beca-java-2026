package br.com.beca.transactions.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Resumo das despesas de um período: total geral e totais por categoria, por dia e por mês. */
public record ExpenseSummary(
        LocalDate from,
        LocalDate to,
        BigDecimal total,
        List<CategoryTotal> byCategory,
        List<DayTotal> byDay,
        List<MonthTotal> byMonth) {

    public record CategoryTotal(Category category, BigDecimal total) {
    }

    public record DayTotal(LocalDate date, BigDecimal total) {
    }

    public record MonthTotal(YearMonth month, BigDecimal total) {
    }
}
