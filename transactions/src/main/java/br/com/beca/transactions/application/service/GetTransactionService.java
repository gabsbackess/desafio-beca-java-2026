package br.com.beca.transactions.application.service;

import br.com.beca.transactions.application.port.in.GetTransactionUseCase;
import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.exception.AccessForbiddenException;
import br.com.beca.transactions.domain.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetTransactionService implements GetTransactionUseCase {

    public static final String ACCESS_DENIED = "Acesso negado.";

    private final TransactionRepositoryPort transactionRepositoryPort;

    public GetTransactionService(TransactionRepositoryPort transactionRepositoryPort) {
        this.transactionRepositoryPort = transactionRepositoryPort;
    }

    @Override
    public Transaction findById(UUID id, UUID requesterId) {
        Transaction transaction = transactionRepositoryPort.findById(id);
        if (!transaction.getUserId().equals(requesterId)) {
            throw new AccessForbiddenException(ACCESS_DENIED);
        }
        return transaction;
    }
}
