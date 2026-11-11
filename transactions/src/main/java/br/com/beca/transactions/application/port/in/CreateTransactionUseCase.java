package br.com.beca.transactions.application.port.in;

import br.com.beca.transactions.domain.model.Transaction;

public interface CreateTransactionUseCase {
    Transaction create(CreateTransactionCommand command);
}
