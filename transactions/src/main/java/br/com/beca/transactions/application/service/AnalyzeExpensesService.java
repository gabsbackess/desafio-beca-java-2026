package br.com.beca.transactions.application.service;

import br.com.beca.transactions.application.port.in.AnalyzeExpensesUseCase;
import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.model.ExpenseAnalyzer;
import br.com.beca.transactions.domain.model.ExpenseSummary;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class AnalyzeExpensesService implements AnalyzeExpensesUseCase {

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final Clock clock;
    private final ExpenseAnalyzer analyzer = new ExpenseAnalyzer();

    public AnalyzeExpensesService(TransactionRepositoryPort transactionRepositoryPort, Clock clock) {
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.clock = clock;
    }

    @Override
    public ExpenseSummary analyze(UUID userId, LocalDate from, LocalDate to) {
        ReportPeriod period = ReportPeriod.resolve(from, to, clock);
        return analyzer.analyze(
                transactionRepositoryPort.findByUser(userId, period.from(), period.to(), null),
                period.from(),
                period.to());
    }
}
