package br.com.beca.transactions.application.port.out;

import java.util.UUID;

public interface UserLookupPort {

    /**
     * Confere no serviço de usuários (MS1) se o usuário existe.
     *
     * @throws br.com.beca.transactions.domain.exception.UserServiceUnavailableException
     *         quando o serviço está fora do ar ou não responde
     */
    boolean exists(UUID userId);
}
