package br.com.beca.transactions.application.service;

import br.com.beca.transactions.support.Ids;
import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.exception.TransactionNotFoundException;
import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionStatus;
import br.com.beca.transactions.domain.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTransactionServiceTest {

    @Mock
    private TransactionRepositoryPort transactionRepositoryPort;

    @InjectMocks
    private GetTransactionService getTransactionService;

    @Test
    void shouldReturnTransactionFoundById() {
        Transaction transaction = new Transaction(
                Ids.USER, Ids.USER, null, TransactionType.DEPOSIT, new BigDecimal("10.00"), Category.OTHER, "",
                TransactionStatus.APPROVED, null, LocalDateTime.of(2026, 11, 10, 9, 0));
        when(transactionRepositoryPort.findById(Ids.USER)).thenReturn(transaction);

        Transaction result = getTransactionService.findById(Ids.USER);

        assertSame(transaction, result);
    }

    @Test
    void shouldPropagateNotFound() {
        when(transactionRepositoryPort.findById(Ids.UNKNOWN))
                .thenThrow(new TransactionNotFoundException("Transação não encontrada."));

        assertThrows(TransactionNotFoundException.class, () -> getTransactionService.findById(Ids.UNKNOWN));
    }
}
