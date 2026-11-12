package br.com.beca.transactions.adapters.in.web.dto;

import br.com.beca.transactions.domain.model.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Valor da transação em outra moeda, com a taxa usada (reais por unidade) e a data da cotação. */
public record ConversionResponse(
        Currency currency,
        BigDecimal rate,
        LocalDate rateDate,
        BigDecimal convertedAmount) {
}
