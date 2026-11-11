package br.com.beca.transactions.adapters.out.auth;

import br.com.beca.transactions.application.port.out.UserAuthenticationPort;
import br.com.beca.transactions.domain.exception.AuthServiceUnavailableException;
import br.com.beca.transactions.domain.exception.InvalidCredentialsException;
import br.com.beca.transactions.domain.model.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Valida e-mail e senha chamando o MS1 (POST /users/authenticate).
 * A senha só passa por aqui em trânsito: não é guardada nem escrita em log.
 */
@Component
public class UsersServiceAuthAdapter implements UserAuthenticationPort {

    private final RestClient restClient;

    public UsersServiceAuthAdapter(@Qualifier("usersRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public AuthenticatedUser authenticate(String email, String password) {
        UsersAuthResponse response;
        try {
            response = restClient.post()
                    .uri("/users/authenticate")
                    .body(Map.of("email", email, "password", password))
                    .retrieve()
                    .body(UsersAuthResponse.class);
        } catch (HttpClientErrorException exception) {
            int status = exception.getStatusCode().value();
            if (status == 401 || status == 400) {
                throw new InvalidCredentialsException("Credenciais inválidas.");
            }
            throw new AuthServiceUnavailableException("Resposta inesperada do serviço de usuários.", exception);
        } catch (RestClientException exception) {
            throw new AuthServiceUnavailableException("Serviço de usuários indisponível.", exception);
        }
        if (response == null || response.id() == null) {
            throw new AuthServiceUnavailableException("Resposta vazia do serviço de usuários.");
        }
        return new AuthenticatedUser(response.id(), response.name(), response.email());
    }
}
