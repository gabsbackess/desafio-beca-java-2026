package br.com.beca.transactions.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(
        UUID userId,
        BigDecimal balance,
        BigDecimal perTransactionLimit,
        BigDecimal dailyLimit) {
}
