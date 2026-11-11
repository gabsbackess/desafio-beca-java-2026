package br.com.beca.transactions.adapters.out.account;

import br.com.beca.transactions.application.port.out.AccountLimitsPort;
import br.com.beca.transactions.domain.exception.AccountNotFoundException;
import br.com.beca.transactions.domain.exception.AccountServiceUnavailableException;
import br.com.beca.transactions.domain.model.AccountLimits;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lê e atualiza saldo e limites do recurso "accounts" do MockAPI.
 * Leitura: GET /accounts?userId=... Atualização: PUT /accounts/{id}.
 * O MockAPI responde 404 quando o filtro não encontra nada, e o filtro é por "contém",
 * por isso o userId é conferido de novo aqui.
 */
@Component
public class MockApiAccountAdapter implements AccountLimitsPort {

    private final RestClient restClient;

    public MockApiAccountAdapter(@Qualifier("accountRestClient") RestClient accountRestClient) {
        this.restClient = accountRestClient;
    }

    @Override
    public AccountLimits findByUserId(UUID userId) {
        return toLimits(fetch(userId));
    }

    @Override
    public void adjustBalance(UUID userId, BigDecimal delta) {
        AccountResponse account = fetch(userId);
        AccountLimits limits = toLimits(account);
        if (account.id() == null || account.id().isBlank()) {
            throw new AccountServiceUnavailableException("Conta sem identificador no serviço de saldo.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", account.userId());
        body.put("balance", limits.balance().add(delta));
        body.put("perTransactionLimit", limits.perTransactionLimit());
        body.put("dailyLimit", limits.dailyLimit());
        try {
            restClient.put()
                    .uri("/accounts/{id}", account.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            throw new AccountNotFoundException("Conta do usuário não encontrada no serviço de saldo.");
        } catch (RestClientException exception) {
            throw new AccountServiceUnavailableException("Serviço de saldo indisponível.", exception);
        }
    }

    private AccountResponse fetch(UUID userId) {
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
        return Arrays.stream(accounts)
                .filter(item -> item.userId() != null && item.userId().equalsIgnoreCase(userId.toString()))
                .findFirst()
                .orElseThrow(() -> new AccountNotFoundException(
                        "Conta do usuário não encontrada no serviço de saldo."));
    }

    private static AccountLimits toLimits(AccountResponse account) {
        if (account.balance() == null || account.perTransactionLimit() == null || account.dailyLimit() == null) {
            throw new AccountServiceUnavailableException("Conta incompleta no serviço de saldo.");
        }
        return new AccountLimits(account.balance(), account.perTransactionLimit(), account.dailyLimit());
    }
}
