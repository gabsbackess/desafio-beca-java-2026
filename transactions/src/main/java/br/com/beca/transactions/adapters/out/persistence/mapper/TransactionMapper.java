package br.com.beca.transactions.adapters.out.persistence.mapper;

import br.com.beca.transactions.adapters.out.persistence.entity.TransactionEntity;
import br.com.beca.transactions.domain.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionEntity toEntity(Transaction transaction) {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(transaction.getId());
        entity.setUserId(transaction.getUserId());
        entity.setDestinationUserId(transaction.getDestinationUserId());
        entity.setType(transaction.getType());
        entity.setAmount(transaction.getAmount());
        entity.setCategory(transaction.getCategory());
        entity.setDescription(transaction.getDescription());
        entity.setStatus(transaction.getStatus());
        entity.setRejectionReason(transaction.getRejectionReason());
        entity.setCreatedAt(transaction.getCreatedAt());
        return entity;
    }

    public Transaction toDomain(TransactionEntity entity) {
        return new Transaction(
                entity.getId(),
                entity.getUserId(),
                entity.getDestinationUserId(),
                entity.getType(),
                entity.getAmount(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getRejectionReason(),
                entity.getCreatedAt());
    }
}
