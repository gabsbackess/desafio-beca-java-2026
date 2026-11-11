package br.com.beca.transactions.adapters.out.account;

import br.com.beca.transactions.application.port.out.AccountLimitsPort;
import br.com.beca.transactions.domain.exception.AccountNotFoundException;
import br.com.beca.transactions.domain.exception.AccountServiceUnavailableException;
import br.com.beca.transactions.domain.model.AccountLimits;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.UUID;

/**
 * Lê saldo e limites do recurso "accounts" do MockAPI (GET /accounts?userId=...).
 * O MockAPI responde 404 quando o filtro não encontra nada, e o filtro é por "contém",
 * por isso o userId é conferido de novo aqui.
 */
@Component
public class MockApiAccountAdapter implements AccountLimitsPort {

    private final RestClient restClient;

    public MockApiAccountAdapter(RestClient accountRestClient) {
        this.restClient = accountRestClient;
    }

    @Override
    public AccountLimits findByUserId(UUID userId) {
        AccountResponse[] accounts;
        try {
            accounts = restClient.get()
                    .uri("/accounts?userId={userId}", userId)
                    .retrieve()
                    .body(AccountResponse[].class);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new AccountNotFoundException("Conta do usuário não encontrada no serviço de saldo.");
        } catch (RestClientException exception) {
            throw new AccountServiceUnavailableException("Serviço de saldo indisponível.", exception);
        }
        if (accounts == null) {
            throw new AccountServiceUnavailableException("Resposta vazia do serviço de saldo.");
        }
        AccountResponse account = Arrays.stream(accounts)
                .filter(item -> item.userId() != null && item.userId().equalsIgnoreCase(userId.toString()))
                .findFirst()
                .orElseThrow(() -> new AccountNotFoundException(
                        "Conta do usuário não encontrada no serviço de saldo."));
        if (account.balance() == null || account.perTransactionLimit() == null || account.dailyLimit() == null) {
            throw new AccountServiceUnavailableException("Conta incompleta no serviço de saldo.");
        }
        return new AccountLimits(account.balance(), account.perTransactionLimit(), account.dailyLimit());
    }
}
