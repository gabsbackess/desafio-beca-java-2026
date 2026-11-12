package br.com.beca.transactions.application.service;

import br.com.beca.transactions.application.port.out.ExchangeRatePort;
import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.exception.AccessForbiddenException;
import br.com.beca.transactions.domain.exception.ExchangeRateUnavailableException;
import br.com.beca.transactions.domain.exception.TransactionNotFoundException;
import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.Currency;
import br.com.beca.transactions.domain.model.ExchangeRate;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionStatus;
import br.com.beca.transactions.domain.model.TransactionType;
import br.com.beca.transactions.support.Ids;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTransactionServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);
    private static final List<ExchangeRate> RATES = List.of(
            new ExchangeRate(Currency.USD, new BigDecimal("5.30"), DAY),
            new ExchangeRate(Currency.EUR, new BigDecimal("6.10"), DAY));

    @Mock
    private TransactionRepositoryPort transactionRepositoryPort;

    @Mock
    private ExchangeRatePort exchangeRatePort;

    @InjectMocks
    private GetTransactionService service;

    private static Transaction transaction(List<ExchangeRate> rates) {
        return new Transaction(Ids.TX_ID, Ids.USER, null, TransactionType.DEPOSIT, new BigDecimal("10.00"),
                Category.OTHER, "", TransactionStatus.APPROVED, null, DAY.atTime(9, 0), rates);
    }

    @Test
    void shouldReturnOwnTransactionThatAlreadyHasRatesWithoutCallingExchangeService() {
        Transaction transaction = transaction(RATES);
        when(transactionRepositoryPort.findById(Ids.TX_ID)).thenReturn(transaction);

        Transaction result = service.findById(Ids.TX_ID, Ids.USER);

        assertSame(transaction, result);
        verifyNoInteractions(exchangeRatePort);
    }

    @Test
    void shouldDenyAccessToTransactionOfAnotherUser() {
        when(transactionRepositoryPort.findById(Ids.TX_ID)).thenReturn(transaction(RATES));

        assertThrows(AccessForbiddenException.class, () -> service.findById(Ids.TX_ID, Ids.OTHER_USER));

        verifyNoInteractions(exchangeRatePort);
    }

    @Test
    void shouldPropagateNotFound() {
        when(transactionRepositoryPort.findById(Ids.UNKNOWN))
                .thenThrow(new TransactionNotFoundException("Transação não encontrada."));

        assertThrows(TransactionNotFoundException.class, () -> service.findById(Ids.UNKNOWN, Ids.USER));
    }

    @Test
    void shouldFillMissingRatesAndSaveThem() {
        when(transactionRepositoryPort.findById(Ids.TX_ID)).thenReturn(transaction(List.of()));
        when(exchangeRatePort.findRates(DAY)).thenReturn(RATES);
        when(transactionRepositoryPort.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = service.findById(Ids.TX_ID, Ids.USER);

        assertEquals(RATES, result.getExchangeRates());
        verify(transactionRepositoryPort).save(any(Transaction.class));
    }

    @Test
    void shouldStillRespondWithoutConversionWhenExchangeServiceIsDown() {
        when(transactionRepositoryPort.findById(Ids.TX_ID)).thenReturn(transaction(List.of()));
        when(exchangeRatePort.findRates(DAY)).thenThrow(new ExchangeRateUnavailableException("fora do ar"));

        Transaction result = service.findById(Ids.TX_ID, Ids.USER);

        assertFalse(result.hasExchangeRates());
        verify(transactionRepositoryPort, never()).save(any(Transaction.class));
    }
}
