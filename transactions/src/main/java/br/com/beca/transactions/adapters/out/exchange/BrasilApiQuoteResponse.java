package br.com.beca.transactions.adapters.out.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/** Resposta de GET /api/cambio/v1/cotacao/{moeda}/{data}. Os campos da BrasilAPI vêm em snake_case (cotacao_venda). Campos não usados são ignorados. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrasilApiQuoteResponse(String moeda, String data, List<Quote> cotacoes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Quote(@JsonProperty("cotacao_compra") BigDecimal cotacaoCompra,
                        @JsonProperty("cotacao_venda") BigDecimal cotacaoVenda,
                        @JsonProperty("tipo_boletim") String tipoBoletim) {
    }
}
