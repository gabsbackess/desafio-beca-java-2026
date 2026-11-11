package br.com.beca.transactions.adapters.out.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/** Cliente HTTP da API Mock, com timeout para não travar a transação quando o serviço está lento. */
@Configuration
public class AccountRestClientConfig {

    @Bean
    public RestClient accountRestClient(
            @Value("${mockapi.url}") String baseUrl,
            @Value("${mockapi.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${mockapi.read-timeout-ms}") long readTimeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        String url = (baseUrl == null || baseUrl.isBlank()) ? "http://localhost:9" : baseUrl.trim();
        String cleanUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return RestClient.builder()
                .baseUrl(cleanUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
