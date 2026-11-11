package br.com.beca.transactions.application.service;

import java.util.UUID;
import br.com.beca.transactions.application.port.in.GetTransactionUseCase;
import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.model.Transaction;
import org.springframework.stereotype.Service;

@Service
public class GetTransactionService implements GetTransactionUseCase {

    private final TransactionRepositoryPort transactionRepositoryPort;

    public GetTransactionService(TransactionRepositoryPort transactionRepositoryPort) {
        this.transactionRepositoryPort = transactionRepositoryPort;
    }

    @Override
    public Transaction findById(UUID id) {
        return transactionRepositoryPort.findById(id);
    }
}
