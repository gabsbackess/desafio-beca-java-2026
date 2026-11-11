package br.com.beca.transactions.application.port.out;

import br.com.beca.transactions.domain.model.AuthenticatedUser;

public interface UserAuthenticationPort {

    /**
     * Confere e-mail e senha no serviço de usuários (MS1).
     *
     * @throws br.com.beca.transactions.domain.exception.InvalidCredentialsException
     *         quando as credenciais não conferem
     * @throws br.com.beca.transactions.domain.exception.AuthServiceUnavailableException
     *         quando o MS1 não responde
     */
    AuthenticatedUser authenticate(String email, String password);
}
