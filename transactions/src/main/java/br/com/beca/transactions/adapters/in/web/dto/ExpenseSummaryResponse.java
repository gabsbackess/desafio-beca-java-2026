package br.com.beca.transactions.adapters.in.web.dto;

import br.com.beca.transactions.domain.model.Category;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseSummaryResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal total,
        List<CategoryTotalResponse> byCategory,
        List<DayTotalResponse> byDay,
        List<MonthTotalResponse> byMonth) {

    public record CategoryTotalResponse(Category category, BigDecimal total) {
    }

    public record DayTotalResponse(LocalDate date, BigDecimal total) {
    }

    /** month no formato AAAA-MM. */
    public record MonthTotalResponse(String month, BigDecimal total) {
    }
}
