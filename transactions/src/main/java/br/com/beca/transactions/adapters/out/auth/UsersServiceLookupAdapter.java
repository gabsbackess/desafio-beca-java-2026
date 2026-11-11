package br.com.beca.transactions.adapters.out.auth;

import br.com.beca.transactions.application.port.out.UserLookupPort;
import br.com.beca.transactions.domain.exception.UserServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/** Confere se um usuário existe chamando o MS1 (GET /users/{id}). */
@Component
public class UsersServiceLookupAdapter implements UserLookupPort {

    private final RestClient restClient;

    public UsersServiceLookupAdapter(@Qualifier("usersRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public boolean exists(UUID userId) {
        try {
            restClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound exception) {
            return false;
        } catch (RestClientException exception) {
            throw new UserServiceUnavailableException("Serviço de usuários indisponível.", exception);
        }
    }
}
