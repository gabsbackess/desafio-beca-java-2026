package br.com.beca.transactions.adapters.out.exchange;

import br.com.beca.transactions.adapters.out.http.RestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ExchangeRestClientConfig {

    @Bean
    public RestClient exchangeRestClient(
            @Value("${brasilapi.url}") String baseUrl,
            @Value("${brasilapi.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${brasilapi.read-timeout-ms}") long readTimeoutMs) {
        return RestClients.create(baseUrl, connectTimeoutMs, readTimeoutMs);
    }
}
