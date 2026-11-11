package br.com.beca.transactions.application.service;

import java.util.UUID;
import br.com.beca.transactions.application.port.in.ListTransactionsUseCase;
import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.exception.InvalidTransactionException;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ListTransactionsService implements ListTransactionsUseCase {

    private final TransactionRepositoryPort transactionRepositoryPort;

    public ListTransactionsService(TransactionRepositoryPort transactionRepositoryPort) {
        this.transactionRepositoryPort = transactionRepositoryPort;
    }

    @Override
    public List<Transaction> list(UUID userId, LocalDate from, LocalDate to, TransactionType type) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidTransactionException("A data inicial não pode ser depois da data final.");
        }
        return transactionRepositoryPort.findByUser(userId, from, to, type);
    }
}
