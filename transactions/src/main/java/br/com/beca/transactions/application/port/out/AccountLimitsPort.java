package br.com.beca.transactions.application.port.out;

import br.com.beca.transactions.domain.model.AccountLimits;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountLimitsPort {

    /**
     * Consulta saldo e limites do usuário no serviço externo (API Mock).
     *
     * @throws br.com.beca.transactions.domain.exception.AccountNotFoundException
     *         quando o usuário não tem conta
     * @throws br.com.beca.transactions.domain.exception.AccountServiceUnavailableException
     *         quando o serviço está fora do ar ou não responde
     */
    AccountLimits findByUserId(UUID userId);

    /**
     * Soma {@code delta} ao saldo da conta do usuário no serviço externo (negativo para saídas,
     * positivo para depósitos). Os limites não mudam.
     *
     * @throws br.com.beca.transactions.domain.exception.AccountNotFoundException
     *         quando o usuário não tem conta
     * @throws br.com.beca.transactions.domain.exception.AccountServiceUnavailableException
     *         quando o serviço está fora do ar ou não aceita a atualização
     */
    void adjustBalance(UUID userId, BigDecimal delta);
}
