package br.com.beca.transactions.application.port.in;

import java.util.UUID;
import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.TransactionType;

import java.math.BigDecimal;

public record CreateTransactionCommand(
        UUID userId,
        TransactionType type,
        BigDecimal amount,
        Category category,
        String description,
        UUID destinationUserId) {
}
