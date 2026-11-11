package br.com.beca.transactions.application.port.in;

import java.util.UUID;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionType;

import java.time.LocalDate;
import java.util.List;

public interface ListTransactionsUseCase {
    /** from, to e type são opcionais (null = sem filtro). */
    List<Transaction> list(UUID userId, LocalDate from, LocalDate to, TransactionType type);
}
