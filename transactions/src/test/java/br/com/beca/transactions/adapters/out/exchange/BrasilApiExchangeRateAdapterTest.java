package br.com.beca.transactions.adapters.out.exchange;

import br.com.beca.transactions.domain.exception.ExchangeRateUnavailableException;
import br.com.beca.transactions.domain.model.Currency;
import br.com.beca.transactions.domain.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrasilApiExchangeRateAdapterTest {

    private static final String BODY = """
            {"moeda":"%s","data":"%s","cotacoes":[
              {"paridade_compra":1.0,"cotacao_compra":%s,"cotacao_venda":%s,"tipo_boletim":"ABERTURA"},
              {"paridade_compra":1.0,"cotacao_compra":%s,"cotacao_venda":%s,"tipo_boletim":"FECHAMENTO PTAX"}]}
            """;

    private MockRestServiceServer server;
    private BrasilApiExchangeRateAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://brasil");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new BrasilApiExchangeRateAdapter(builder.build());
    }

    private static String quote(String currency, String day, String open, String close) {
        return BODY.formatted(currency, day, open, open, close, close);
    }

    @Test
    void shouldUseTheSaleQuoteOfTheLastBulletinForDollarAndEuro() {
        server.expect(requestTo("http://brasil/api/cambio/v1/cotacao/USD/2026-11-10"))
                .andRespond(withSuccess(quote("USD", "2026-11-10", "5.30", "5.33"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://brasil/api/cambio/v1/cotacao/EUR/2026-11-10"))
                .andRespond(withSuccess(quote("EUR", "2026-11-10", "6.10", "6.15"), MediaType.APPLICATION_JSON));

        List<ExchangeRate> rates = adapter.findRates(LocalDate.of(2026, 11, 10));

        assertEquals(2, rates.size());
        assertEquals(Currency.USD, rates.get(0).currency());
        assertEquals(0, new BigDecimal("5.33").compareTo(rates.get(0).rate()));
        assertEquals(Currency.EUR, rates.get(1).currency());
        assertEquals(0, new BigDecimal("6.15").compareTo(rates.get(1).rate()));
        assertEquals(LocalDate.of(2026, 11, 10), rates.get(0).date());
        server.verify();
    }

    @Test
    void shouldFallBackToPreviousDayWhenTheApiRefusesTodaysQuote() {
        String refused = "{\"message\":\"Nao e possivel consultar a cotacao do dia de hoje\",\"type\":\"today_date_error\",\"name\":\"NO_TODAY_DATE\"}";
        server.expect(requestTo("http://brasil/api/cambio/v1/cotacao/USD/2026-11-12"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(refused));
        server.expect(requestTo("http://brasil/api/cambio/v1/cotacao/USD/2026-11-11"))
                .andRespond(withSuccess(quote("USD", "2026-11-11", "5.55", "5.56"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://brasil/api/cambio/v1/cotacao/EUR/2026-11-12"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(refused));
        server.expect(requestTo("http://brasil/api/cambio/v1/cotacao/EUR/2026-11-11"))
                .andRespond(withSuccess(quote("EUR", "2026-11-11", "6.40", "6.45"), MediaType.APPLICATION_JSON));

        List<ExchangeRate> rates = adapter.findRates(LocalDate.of(2026, 11, 12));

        assertEquals(LocalDate.of(2026, 11, 11), rates.get(0).date());
        assertEquals(0, new BigDecimal("5.56").compareTo(rates.get(0).rate()));
        assertEquals(0, new BigDecimal("6.45").compareTo(rates.get(1).rate()));
        server.verify();
    }

    @Test
    void shouldFallBackToPreviousDayWhenThereIsNoQuoteOnTheRequestedDay() {
        server.expect(requestTo("http://brasil/api/cambio/v1/cotacao/USD/2026-11-08"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://brasil/api/cambio/v1/cotacao/USD/2026-11-07"))
                .andRespond(withSuccess(quote("USD", "2026-11-07", "5.20", "5.25"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://brasil/api/cambio/v1/cotacao/EUR/2026-11-08"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://brasil/api/cambio/v1/cotacao/EUR/2026-11-07"))
                .andRespond(withSuccess(quote("EUR", "2026-11-07", "6.00", "6.05"), MediaType.APPLICATION_JSON));

        List<ExchangeRate> rates = adapter.findRates(LocalDate.of(2026, 11, 8));

        assertEquals(LocalDate.of(2026, 11, 7), rates.get(0).date());
        assertEquals(LocalDate.of(2026, 11, 7), rates.get(1).date());
        server.verify();
    }

    @Test
    void shouldThrowUnavailableWhenBrasilApiFails() {
        server.expect(requestTo("http://brasil/api/cambio/v1/cotacao/USD/2026-11-10"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ExchangeRateUnavailableException.class, () -> adapter.findRates(LocalDate.of(2026, 11, 10)));
    }

    @Test
    void shouldThrowUnavailableWhenResponseHasNoQuotes() {
        for (int i = 0; i <= 7; i++) {
            LocalDate day = LocalDate.of(2026, 11, 10).minusDays(i);
            server.expect(requestTo("http://brasil/api/cambio/v1/cotacao/USD/" + day))
                    .andRespond(withSuccess("{\"moeda\":\"USD\",\"cotacoes\":[]}", MediaType.APPLICATION_JSON));
        }

        assertThrows(ExchangeRateUnavailableException.class, () -> adapter.findRates(LocalDate.of(2026, 11, 10)));
    }
}
