package br.com.beca.transactions.domain.model;

import br.com.beca.transactions.domain.model.ExpenseSummary.CategoryTotal;
import br.com.beca.transactions.support.Ids;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseAnalyzerTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 30);

    private final ExpenseAnalyzer analyzer = new ExpenseAnalyzer();

    private static Transaction tx(TransactionType type, String amount, Category category,
                                  TransactionStatus status, LocalDateTime when) {
        return new Transaction(null, Ids.USER, type == TransactionType.TRANSFER ? Ids.OTHER_USER : null, type,
                new BigDecimal(amount), category, "", status, status == TransactionStatus.REJECTED ? "x" : null, when);
    }

    private static Transaction purchase(String amount, Category category, int day) {
        return tx(TransactionType.PURCHASE, amount, category, TransactionStatus.APPROVED,
                LocalDateTime.of(2026, 11, day, 12, 0));
    }

    @Test
    void shouldShowThreeCategoryLinesAndTotalEqualToTheSumWithFivePurchasesInThreeCategories() {
        List<Transaction> transactions = List.of(
                purchase("100.00", Category.FOOD, 2),
                purchase("50.50", Category.FOOD, 3),
                purchase("200.00", Category.TRANSPORT, 3),
                purchase("30.00", Category.LEISURE, 10),
                purchase("19.50", Category.TRANSPORT, 15));

        ExpenseSummary summary = analyzer.analyze(transactions, FROM, TO);

        assertEquals(3, summary.byCategory().size());
        assertEquals(new BigDecimal("400.00"), summary.total());
        BigDecimal sumOfCategories = summary.byCategory().stream()
                .map(CategoryTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(summary.total(), sumOfCategories);
    }

    @Test
    void shouldOrderCategoriesFromBiggestToSmallest() {
        ExpenseSummary summary = analyzer.analyze(List.of(
                purchase("10.00", Category.LEISURE, 2),
                purchase("300.00", Category.HOUSING, 2),
                purchase("50.00", Category.FOOD, 2)), FROM, TO);

        assertEquals(List.of(Category.HOUSING, Category.FOOD, Category.LEISURE),
                summary.byCategory().stream().map(CategoryTotal::category).toList());
    }

    @Test
    void shouldIgnoreRejectedTransactionsAndDeposits() {
        List<Transaction> transactions = List.of(
                purchase("100.00", Category.FOOD, 2),
                tx(TransactionType.PURCHASE, "999.00", Category.FOOD, TransactionStatus.REJECTED,
                        LocalDateTime.of(2026, 11, 2, 9, 0)),
                tx(TransactionType.DEPOSIT, "5000.00", Category.OTHER, TransactionStatus.APPROVED,
                        LocalDateTime.of(2026, 11, 2, 9, 0)));

        ExpenseSummary summary = analyzer.analyze(transactions, FROM, TO);

        assertEquals(new BigDecimal("100.00"), summary.total());
        assertEquals(1, summary.byCategory().size());
    }

    @Test
    void shouldCountWithdrawalsAndSentTransfersAsExpenses() {
        List<Transaction> transactions = List.of(
                tx(TransactionType.WITHDRAWAL, "70.00", Category.OTHER, TransactionStatus.APPROVED,
                        LocalDateTime.of(2026, 11, 4, 9, 0)),
                tx(TransactionType.TRANSFER, "30.00", Category.OTHER, TransactionStatus.APPROVED,
                        LocalDateTime.of(2026, 11, 4, 10, 0)));

        ExpenseSummary summary = analyzer.analyze(transactions, FROM, TO);

        assertEquals(new BigDecimal("100.00"), summary.total());
        assertEquals(new BigDecimal("100.00"), summary.byCategory().get(0).total());
    }

    @Test
    void shouldGroupByDayAndByMonthInChronologicalOrder() {
        List<Transaction> transactions = List.of(
                purchase("10.00", Category.FOOD, 5),
                purchase("20.00", Category.FOOD, 5),
                purchase("5.00", Category.FOOD, 1),
                tx(TransactionType.PURCHASE, "40.00", Category.FOOD, TransactionStatus.APPROVED,
                        LocalDateTime.of(2026, 12, 2, 8, 0)));

        ExpenseSummary summary = analyzer.analyze(transactions, FROM, LocalDate.of(2026, 12, 31));

        assertEquals(List.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), LocalDate.of(2026, 12, 2)),
                summary.byDay().stream().map(ExpenseSummary.DayTotal::date).toList());
        assertEquals(new BigDecimal("30.00"), summary.byDay().get(1).total());
        assertEquals(2, summary.byMonth().size());
        assertEquals(YearMonth.of(2026, 11), summary.byMonth().get(0).month());
        assertEquals(new BigDecimal("35.00"), summary.byMonth().get(0).total());
        assertEquals(new BigDecimal("40.00"), summary.byMonth().get(1).total());
    }

    @Test
    void shouldIgnoreTransactionsOutsideThePeriod() {
        List<Transaction> transactions = List.of(
                purchase("10.00", Category.FOOD, 5),
                tx(TransactionType.PURCHASE, "99.00", Category.FOOD, TransactionStatus.APPROVED,
                        LocalDateTime.of(2026, 10, 31, 23, 59)));

        assertEquals(new BigDecimal("10.00"), analyzer.analyze(transactions, FROM, TO).total());
    }

    @Test
    void shouldReturnZeroAndEmptyListsWhenThereAreNoExpenses() {
        ExpenseSummary summary = analyzer.analyze(List.of(), FROM, TO);

        assertEquals(new BigDecimal("0.00"), summary.total());
        assertTrue(summary.byCategory().isEmpty());
        assertTrue(summary.byDay().isEmpty());
        assertTrue(summary.byMonth().isEmpty());
    }
}
