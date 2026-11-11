package br.com.beca.transactions.application.port.in;

import java.util.UUID;
import br.com.beca.transactions.domain.model.Transaction;

public interface GetTransactionUseCase {
    Transaction findById(UUID id);
}
