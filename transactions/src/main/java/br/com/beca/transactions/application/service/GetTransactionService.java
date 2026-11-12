package br.com.beca.transactions.application.service;

import br.com.beca.transactions.application.port.in.GetTransactionUseCase;
import br.com.beca.transactions.application.port.out.ExchangeRatePort;
import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.exception.AccessForbiddenException;
import br.com.beca.transactions.domain.exception.ExchangeRateUnavailableException;
import br.com.beca.transactions.domain.model.ExchangeRate;
import br.com.beca.transactions.domain.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetTransactionService implements GetTransactionUseCase {

    public static final String ACCESS_DENIED = "Acesso negado.";

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final ExchangeRatePort exchangeRatePort;

    public GetTransactionService(TransactionRepositoryPort transactionRepositoryPort,
                                 ExchangeRatePort exchangeRatePort) {
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.exchangeRatePort = exchangeRatePort;
    }

    @Override
    public Transaction findById(UUID id, UUID requesterId) {
        Transaction transaction = transactionRepositoryPort.findById(id);
        if (!transaction.getUserId().equals(requesterId)) {
            throw new AccessForbiddenException(ACCESS_DENIED);
        }
        if (transaction.hasExchangeRates()) {
            return transaction;
        }
        // A conversão falhou na criação: tenta de novo agora. Se ainda falhar, a consulta segue sem conversão.
        try {
            List<ExchangeRate> rates = exchangeRatePort.findRates(transaction.getCreatedAt().toLocalDate());
            return transactionRepositoryPort.save(transaction.withExchangeRates(rates));
        } catch (ExchangeRateUnavailableException exception) {
            return transaction;
        }
    }
}
