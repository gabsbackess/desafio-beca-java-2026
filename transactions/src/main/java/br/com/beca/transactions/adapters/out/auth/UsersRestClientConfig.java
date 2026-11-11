package br.com.beca.transactions.adapters.out.auth;

import br.com.beca.transactions.adapters.out.http.RestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UsersRestClientConfig {

    /** No Compose a URL é http://users:8081 (nome do serviço, não localhost). */
    @Bean
    public RestClient usersRestClient(
            @Value("${users.url}") String baseUrl,
            @Value("${users.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${users.read-timeout-ms}") long readTimeoutMs) {
        return RestClients.create(baseUrl, connectTimeoutMs, readTimeoutMs);
    }
}
