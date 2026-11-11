package br.com.beca.transactions.application.service;

import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.exception.AccessForbiddenException;
import br.com.beca.transactions.domain.exception.TransactionNotFoundException;
import br.com.beca.transactions.domain.model.Category;
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
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTransactionServiceTest {

    @Mock
    private TransactionRepositoryPort transactionRepositoryPort;

    @InjectMocks
    private GetTransactionService service;

    private static Transaction transaction() {
        return new Transaction(Ids.TX_ID, Ids.USER, null, TransactionType.DEPOSIT, new BigDecimal("10.00"),
                Category.OTHER, "", TransactionStatus.APPROVED, null, LocalDateTime.of(2026, 11, 10, 9, 0));
    }

    @Test
    void shouldReturnTheTransactionOfTheRequester() {
        Transaction transaction = transaction();
        when(transactionRepositoryPort.findById(Ids.TX_ID)).thenReturn(transaction);

        assertSame(transaction, service.findById(Ids.TX_ID, Ids.USER));
    }

    @Test
    void shouldDenyAccessToTransactionOfAnotherUser() {
        when(transactionRepositoryPort.findById(Ids.TX_ID)).thenReturn(transaction());

        assertThrows(AccessForbiddenException.class, () -> service.findById(Ids.TX_ID, Ids.OTHER_USER));
    }

    @Test
    void shouldPropagateNotFound() {
        when(transactionRepositoryPort.findById(Ids.UNKNOWN))
                .thenThrow(new TransactionNotFoundException("Transação não encontrada."));

        assertThrows(TransactionNotFoundException.class, () -> service.findById(Ids.UNKNOWN, Ids.USER));
    }
}
