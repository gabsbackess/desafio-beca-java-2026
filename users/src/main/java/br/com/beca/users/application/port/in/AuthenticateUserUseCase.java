package br.com.beca.users.application.port.in;

import br.com.beca.users.domain.model.User;

public interface AuthenticateUserUseCase {
    /**
     * Confere e-mail e senha. Devolve o usuário autenticado ou lança
     * InvalidCredentialsException (mesma mensagem para e-mail inexistente e senha errada).
     */
    User authenticate(String email, String rawPassword);
}
