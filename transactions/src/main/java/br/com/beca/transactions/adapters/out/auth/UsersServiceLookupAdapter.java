package br.com.beca.transactions.adapters.out.auth;

import br.com.beca.transactions.application.port.out.UserLookupPort;
import br.com.beca.transactions.domain.exception.UserServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * Confere se um usuário existe chamando o MS1 (GET /users/{id}). A rota do MS1 exige autenticação; o MS2 se
 * identifica como serviço com a chave interna (cabeçalho X-Internal-Key), sem repassar a senha de ninguém.
 */
@Component
public class UsersServiceLookupAdapter implements UserLookupPort {

    static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

    private final RestClient restClient;
    private final String internalKey;

    public UsersServiceLookupAdapter(@Qualifier("usersRestClient") RestClient restClient,
                                     @Value("${users.internal-key:}") String internalKey) {
        this.restClient = restClient;
        this.internalKey = internalKey;
    }

    @Override
    public boolean exists(UUID userId) {
        try {
            restClient.get()
                    .uri("/users/{id}", userId)
                    .header(INTERNAL_KEY_HEADER, internalKey)
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
