package br.com.beca.transactions.application.service;

import br.com.beca.transactions.support.Ids;
import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.exception.InvalidTransactionException;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTransactionsServiceTest {

    @Mock
    private TransactionRepositoryPort transactionRepositoryPort;

    @InjectMocks
    private ListTransactionsService listTransactionsService;

    @Test
    void shouldPassFiltersToRepository() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);
        when(transactionRepositoryPort.findByUser(Ids.USER, from, to, TransactionType.PURCHASE))
                .thenReturn(List.of());

        List<Transaction> result =
                listTransactionsService.list(Ids.USER, from, to, TransactionType.PURCHASE);

        assertEquals(0, result.size());
        verify(transactionRepositoryPort).findByUser(Ids.USER, from, to, TransactionType.PURCHASE);
    }

    @Test
    void shouldAcceptListWithoutFilters() {
        when(transactionRepositoryPort.findByUser(Ids.USER, null, null, null)).thenReturn(List.of());

        List<Transaction> result = listTransactionsService.list(Ids.USER, null, null, null);

        assertEquals(0, result.size());
    }

    @Test
    void shouldRejectPeriodWhereStartIsAfterEnd() {
        LocalDate from = LocalDate.of(2026, 12, 1);
        LocalDate to = LocalDate.of(2026, 11, 1);

        assertThrows(InvalidTransactionException.class,
                () -> listTransactionsService.list(Ids.USER, from, to, null));

        verifyNoInteractions(transactionRepositoryPort);
    }
}
