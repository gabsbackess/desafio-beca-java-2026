package br.com.beca.transactions.application.port.in;

import br.com.beca.transactions.domain.model.Transaction;

import java.util.UUID;

public interface GetTransactionUseCase {
    /**
     * Consulta uma transação do próprio usuário.
     *
     * @throws br.com.beca.transactions.domain.exception.AccessForbiddenException
     *         quando a transação é de outro usuário
     */
    Transaction findById(UUID id, UUID requesterId);
}
