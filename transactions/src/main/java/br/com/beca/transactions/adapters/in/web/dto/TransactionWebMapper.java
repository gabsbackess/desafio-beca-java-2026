package br.com.beca.transactions.adapters.in.web.dto;

import br.com.beca.transactions.application.port.in.CreateTransactionCommand;
import br.com.beca.transactions.domain.model.AccountLimits;
import br.com.beca.transactions.domain.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TransactionWebMapper {

    public static final String CONVERSION_UNAVAILABLE =
            "Conversão indisponível no momento: o serviço de câmbio não respondeu.";

    public CreateTransactionCommand toCommand(CreateTransactionRequest request, UUID userId) {
        return new CreateTransactionCommand(
                userId,
                request.type(),
                request.amount(),
                request.category(),
                request.description(),
                request.destinationUserId());
    }

    public TransactionResponse toResponse(Transaction transaction) {
        List<ConversionResponse> conversions = transaction.getExchangeRates().stream()
                .map(rate -> new ConversionResponse(
                        rate.currency(), rate.rate(), rate.date(), rate.convert(transaction.getAmount())))
                .toList();
        boolean available = !conversions.isEmpty();
        return new TransactionResponse(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getDestinationUserId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCategory(),
                transaction.getDescription(),
                transaction.getStatus(),
                transaction.getRejectionReason(),
                transaction.getCreatedAt(),
                available,
                available ? null : CONVERSION_UNAVAILABLE,
                conversions);
    }

    public BalanceResponse toBalanceResponse(UUID userId, AccountLimits limits) {
        return new BalanceResponse(userId, limits.balance(), limits.perTransactionLimit(), limits.dailyLimit());
    }
}
