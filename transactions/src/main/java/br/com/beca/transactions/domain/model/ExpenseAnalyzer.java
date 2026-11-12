package br.com.beca.transactions.domain.model;

import br.com.beca.transactions.domain.model.ExpenseSummary.CategoryTotal;
import br.com.beca.transactions.domain.model.ExpenseSummary.DayTotal;
import br.com.beca.transactions.domain.model.ExpenseSummary.MonthTotal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Análise de despesas. Domínio puro: nada de Spring, JPA ou HTTP.
 * Só conta como despesa a transação APROVADA de saída: retirada, transferência enviada e compra.
 * Depósitos e transações rejeitadas ficam de fora.
 */
public class ExpenseAnalyzer {

    public ExpenseSummary analyze(List<Transaction> transactions, LocalDate from, LocalDate to) {
        Map<Category, BigDecimal> byCategory = new EnumMap<>(Category.class);
        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
        Map<YearMonth, BigDecimal> byMonth = new TreeMap<>();
        BigDecimal total = money(BigDecimal.ZERO);

        for (Transaction transaction : transactions) {
            if (!isExpense(transaction)) {
                continue;
            }
            LocalDate day = transaction.getCreatedAt().toLocalDate();
            if (day.isBefore(from) || day.isAfter(to)) {
                continue;
            }
            BigDecimal amount = transaction.getAmount();
            total = total.add(amount);
            byCategory.merge(transaction.getCategory(), amount, BigDecimal::add);
            byDay.merge(day, amount, BigDecimal::add);
            byMonth.merge(YearMonth.from(day), amount, BigDecimal::add);
        }

        List<CategoryTotal> categories = byCategory.entrySet().stream()
                .map(entry -> new CategoryTotal(entry.getKey(), money(entry.getValue())))
                .sorted(Comparator.comparing(CategoryTotal::total).reversed()
                        .thenComparing(item -> item.category().name()))
                .toList();
        List<DayTotal> days = byDay.entrySet().stream()
                .map(entry -> new DayTotal(entry.getKey(), money(entry.getValue())))
                .toList();
        List<MonthTotal> months = byMonth.entrySet().stream()
                .map(entry -> new MonthTotal(entry.getKey(), money(entry.getValue())))
                .toList();

        return new ExpenseSummary(from, to, money(total), categories, days, months);
    }

    private static boolean isExpense(Transaction transaction) {
        return transaction.isApproved() && transaction.getType().isOutflow();
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
