package br.com.beca.transactions.application.port.out;

import br.com.beca.transactions.domain.model.ExchangeRate;

import java.time.LocalDate;
import java.util.List;

public interface ExchangeRatePort {

    /**
     * Taxas (dólar e euro) da data informada. Se não houver cotação no dia (fim de semana, feriado),
     * devolve a do dia útil anterior mais próximo, com a data real da cotação.
     *
     * @throws br.com.beca.transactions.domain.exception.ExchangeRateUnavailableException
     *         quando o serviço de câmbio está fora do ar ou não tem cotação
     */
    List<ExchangeRate> findRates(LocalDate date);
}
