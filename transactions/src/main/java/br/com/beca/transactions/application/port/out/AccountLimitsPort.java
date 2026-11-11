package br.com.beca.transactions.application.port.out;

import java.util.UUID;
import br.com.beca.transactions.domain.model.AccountLimits;

public interface AccountLimitsPort {

    /**
     * Consulta saldo e limites do usuário no serviço externo (API Mock).
     *
     * @throws br.com.beca.transactions.domain.exception.AccountServiceUnavailableException
     *         quando o serviço está fora do ar ou não responde
     */
    AccountLimits findByUserId(UUID userId);
}
