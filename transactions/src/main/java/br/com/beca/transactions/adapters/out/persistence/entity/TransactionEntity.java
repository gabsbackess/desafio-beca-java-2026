package br.com.beca.transactions.adapters.out.persistence.entity;

import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.TransactionStatus;
import br.com.beca.transactions.domain.model.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = @Index(name = "idx_transactions_user_created", columnList = "userId, createdAt"))
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private UUID destinationUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    private String rejectionReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(precision = 19, scale = 6)
    private BigDecimal usdRate;

    private LocalDate usdRateDate;

    @Column(precision = 19, scale = 6)
    private BigDecimal eurRate;

    private LocalDate eurRateDate;

    public TransactionEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getDestinationUserId() { return destinationUserId; }
    public void setDestinationUserId(UUID destinationUserId) { this.destinationUserId = destinationUserId; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public BigDecimal getUsdRate() { return usdRate; }
    public void setUsdRate(BigDecimal usdRate) { this.usdRate = usdRate; }
    public LocalDate getUsdRateDate() { return usdRateDate; }
    public void setUsdRateDate(LocalDate usdRateDate) { this.usdRateDate = usdRateDate; }
    public BigDecimal getEurRate() { return eurRate; }
    public void setEurRate(BigDecimal eurRate) { this.eurRate = eurRate; }
    public LocalDate getEurRateDate() { return eurRateDate; }
    public void setEurRateDate(LocalDate eurRateDate) { this.eurRateDate = eurRateDate; }
}
