package br.com.beca.transactions.adapters.in.web.dto;

import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.TransactionStatus;
import br.com.beca.transactions.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID userId,
        UUID destinationUserId,
        TransactionType type,
        BigDecimal amount,
        Category category,
        String description,
        TransactionStatus status,
        String rejectionReason,
        LocalDateTime createdAt) {
}
