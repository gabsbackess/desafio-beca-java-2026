package br.com.beca.transactions.application.port.in;

import br.com.beca.transactions.domain.model.AccountLimits;

import java.util.UUID;

public interface GetAccountUseCase {
    /** Saldo e limites atuais do usuário, vindos da API Mock. */
    AccountLimits getAccount(UUID userId);
}
