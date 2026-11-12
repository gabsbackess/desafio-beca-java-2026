package br.com.beca.transactions.application.port.in;

import br.com.beca.transactions.domain.model.ExpenseSummary;

import java.time.LocalDate;
import java.util.UUID;

public interface AnalyzeExpensesUseCase {
    /** from e to são opcionais; sem eles vale o mês atual. */
    ExpenseSummary analyze(UUID userId, LocalDate from, LocalDate to);
}
