package br.com.beca.transactions.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

public interface GenerateTransactionsReportUseCase {
    /** CSV (texto) com as transações do usuário no período. from e to são opcionais; sem eles vale o mês atual. */
    String generateCsv(UUID userId, LocalDate from, LocalDate to);
}
