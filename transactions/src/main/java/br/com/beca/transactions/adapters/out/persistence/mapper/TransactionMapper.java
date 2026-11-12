package br.com.beca.transactions.adapters.out.persistence.mapper;

import br.com.beca.transactions.adapters.out.persistence.entity.TransactionEntity;
import br.com.beca.transactions.domain.model.Currency;
import br.com.beca.transactions.domain.model.ExchangeRate;
import br.com.beca.transactions.domain.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
        for (ExchangeRate rate : transaction.getExchangeRates()) {
            if (rate.currency() == Currency.USD) {
                entity.setUsdRate(rate.rate());
                entity.setUsdRateDate(rate.date());
            } else if (rate.currency() == Currency.EUR) {
                entity.setEurRate(rate.rate());
                entity.setEurRateDate(rate.date());
            }
        }
        return entity;
    }

    public Transaction toDomain(TransactionEntity entity) {
        List<ExchangeRate> rates = new ArrayList<>();
        if (entity.getUsdRate() != null && entity.getUsdRateDate() != null) {
            rates.add(new ExchangeRate(Currency.USD, entity.getUsdRate(), entity.getUsdRateDate()));
        }
        if (entity.getEurRate() != null && entity.getEurRateDate() != null) {
            rates.add(new ExchangeRate(Currency.EUR, entity.getEurRate(), entity.getEurRateDate()));
        }
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
                entity.getCreatedAt(),
                rates);
    }
}
