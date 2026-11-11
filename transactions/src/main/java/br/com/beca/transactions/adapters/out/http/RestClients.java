package br.com.beca.transactions.adapters.out.http;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/** Cria clientes HTTP com timeout, para que um serviço lento não trave o MS2. */
public final class RestClients {

    private static final String FALLBACK_URL = "http://localhost:9";

    private RestClients() {
    }

    public static RestClient create(String baseUrl, long connectTimeoutMs, long readTimeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        String url = (baseUrl == null || baseUrl.isBlank()) ? FALLBACK_URL : baseUrl.trim();
        String cleanUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return RestClient.builder().baseUrl(cleanUrl).requestFactory(requestFactory).build();
    }
}
