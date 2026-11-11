package br.com.beca.transactions.adapters.out.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Um item do recurso "accounts" do MockAPI. Campos extras são ignorados. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountResponse(
        String id,
        String userId,
        BigDecimal balance,
        BigDecimal perTransactionLimit,
        BigDecimal dailyLimit) {
}
