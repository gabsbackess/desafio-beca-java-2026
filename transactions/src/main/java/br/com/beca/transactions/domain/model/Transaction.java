package br.com.beca.transactions.domain.model;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Transaction {
    private UUID id;
    private UUID userId;
    private UUID destinationUserId;
    private TransactionType type;
    private BigDecimal amount;
    private Category category;
    private String description;
    private TransactionStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private List<ExchangeRate> exchangeRates = List.of();

    public Transaction() {
    }

    public Transaction(UUID id, UUID userId, UUID destinationUserId, TransactionType type,
                       BigDecimal amount, Category category, String description,
                       TransactionStatus status, String rejectionReason, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.destinationUserId = destinationUserId;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
    }

    public Transaction(UUID id, UUID userId, UUID destinationUserId, TransactionType type,
                       BigDecimal amount, Category category, String description,
                       TransactionStatus status, String rejectionReason, LocalDateTime createdAt,
                       List<ExchangeRate> exchangeRates) {
        this(id, userId, destinationUserId, type, amount, category, description,
                status, rejectionReason, createdAt);
        this.exchangeRates = exchangeRates == null ? List.of() : List.copyOf(exchangeRates);
    }

    /** Cópia desta transação com as taxas de câmbio informadas. */
    public Transaction withExchangeRates(List<ExchangeRate> rates) {
        return new Transaction(id, userId, destinationUserId, type, amount, category, description,
                status, rejectionReason, createdAt, rates);
    }

    public boolean hasExchangeRates() {
        return !exchangeRates.isEmpty();
    }

    public List<ExchangeRate> getExchangeRates() {
        return exchangeRates;
    }

    /** Cria uma transação nova (sem id) a partir do pedido e da decisão. */
    public static Transaction from(TransactionRequest request, ApprovalDecision decision,
                                   LocalDateTime createdAt) {
        return new Transaction(
                null,
                request.userId(),
                request.destinationUserId(),
                request.type(),
                request.amount(),
                request.category(),
                request.description(),
                decision.status(),
                decision.rejectionReason(),
                createdAt);
    }

    public boolean isApproved() {
        return status == TransactionStatus.APPROVED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getDestinationUserId() {
        return destinationUserId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Category getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
