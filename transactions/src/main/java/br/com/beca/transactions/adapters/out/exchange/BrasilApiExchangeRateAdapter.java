package br.com.beca.transactions.adapters.out.exchange;

import br.com.beca.transactions.application.port.out.ExchangeRatePort;
import br.com.beca.transactions.domain.exception.ExchangeRateUnavailableException;
import br.com.beca.transactions.domain.model.Currency;
import br.com.beca.transactions.domain.model.ExchangeRate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Cotações PTAX do Banco Central pela BrasilAPI:
 * GET /api/cambio/v1/cotacao/{moeda}/{data} (data no formato AAAA-MM-DD).
 * Usa a cotação de venda do último boletim do dia (o de fechamento, quando existe).
 * Em fim de semana, feriado e no dia corrente (a BrasilAPI responde 400 "NO_TODAY_DATE") não há cotação:
 * volta dia a dia, até 7 dias.
 */
@Component
public class BrasilApiExchangeRateAdapter implements ExchangeRatePort {

    private static final int MAX_DAYS_BACK = 7;

    private final RestClient restClient;

    public BrasilApiExchangeRateAdapter(@Qualifier("exchangeRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<ExchangeRate> findRates(LocalDate date) {
        List<ExchangeRate> rates = new ArrayList<>();
        for (Currency currency : Currency.values()) {
            rates.add(findRate(currency, date));
        }
        return rates;
    }

    private ExchangeRate findRate(Currency currency, LocalDate date) {
        for (int daysBack = 0; daysBack <= MAX_DAYS_BACK; daysBack++) {
            LocalDate day = date.minusDays(daysBack);
            try {
                BrasilApiQuoteResponse response = restClient.get()
                        .uri("/api/cambio/v1/cotacao/{moeda}/{data}", currency.name(), day.toString())
                        .retrieve()
                        .body(BrasilApiQuoteResponse.class);
                BigDecimal rate = lastSaleQuote(response);
                if (rate != null) {
                    return new ExchangeRate(currency, rate, day);
                }
            } catch (HttpClientErrorException.NotFound | HttpClientErrorException.BadRequest exception) {
                // Sem cotação nesse dia (404) ou dia ainda não liberado pela BrasilAPI (400, "NO_TODAY_DATE":
                // ela não entrega a cotação do dia corrente): tenta o dia anterior.
            } catch (RestClientException exception) {
                throw new ExchangeRateUnavailableException("Serviço de câmbio indisponível.", exception);
            }
        }
        throw new ExchangeRateUnavailableException("Sem cotação de " + currency + " perto de " + date + ".");
    }

    private static BigDecimal lastSaleQuote(BrasilApiQuoteResponse response) {
        if (response == null || response.cotacoes() == null) {
            return null;
        }
        List<BrasilApiQuoteResponse.Quote> quotes = response.cotacoes();
        for (int i = quotes.size() - 1; i >= 0; i--) {
            BigDecimal sale = quotes.get(i).cotacaoVenda();
            if (sale != null && sale.signum() > 0) {
                return sale;
            }
        }
        return null;
    }
}
