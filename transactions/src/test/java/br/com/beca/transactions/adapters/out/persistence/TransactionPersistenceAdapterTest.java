package br.com.beca.transactions.adapters.out.persistence;

import br.com.beca.transactions.adapters.out.persistence.entity.TransactionEntity;
import br.com.beca.transactions.adapters.out.persistence.mapper.TransactionMapper;
import br.com.beca.transactions.adapters.out.persistence.repository.TransactionJpaRepository;
import br.com.beca.transactions.domain.exception.TransactionNotFoundException;
import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionStatus;
import br.com.beca.transactions.domain.model.TransactionType;
import br.com.beca.transactions.support.Ids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionPersistenceAdapterTest {

    @Mock
    private TransactionJpaRepository repository;

    private TransactionPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TransactionPersistenceAdapter(repository, new TransactionMapper());
    }

    private static Transaction newTransaction() {
        return new Transaction(null, Ids.USER, null, TransactionType.PURCHASE, new BigDecimal("50.00"),
                Category.FOOD, "mercado", TransactionStatus.APPROVED, null,
                LocalDateTime.of(2026, 11, 10, 9, 0));
    }

    @Test
    void shouldSaveAndReturnTransactionWithGeneratedId() {
        when(repository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
            TransactionEntity entity = invocation.getArgument(0);
            entity.setId(Ids.TX_ID);
            return entity;
        });

        Transaction saved = adapter.save(newTransaction());

        assertEquals(Ids.TX_ID, saved.getId());
        assertEquals(Category.FOOD, saved.getCategory());
        assertEquals(TransactionStatus.APPROVED, saved.getStatus());
    }

    @Test
    void shouldThrowNotFoundWhenIdDoesNotExist() {
        when(repository.findById(Ids.UNKNOWN)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> adapter.findById(Ids.UNKNOWN));
    }

    @Test
    void shouldReturnZeroWhenThereAreNoApprovedExpensesInTheDay() {
        when(repository.sumAmount(eq(Ids.USER), eq(TransactionStatus.APPROVED), any(), any(), any()))
                .thenReturn(null);

        assertEquals(BigDecimal.ZERO, adapter.sumApprovedExpensesOn(Ids.USER, LocalDate.of(2026, 11, 10)));
    }

    @Test
    void shouldSumOnlyOutflowTypesWithinTheDay() {
        LocalDate day = LocalDate.of(2026, 11, 10);
        when(repository.sumAmount(any(), any(), any(), any(), any())).thenReturn(new BigDecimal("120.00"));

        BigDecimal result = adapter.sumApprovedExpensesOn(Ids.USER, day);

        assertEquals(new BigDecimal("120.00"), result);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<TransactionType>> types = ArgumentCaptor.forClass(Collection.class);
        org.mockito.Mockito.verify(repository).sumAmount(eq(Ids.USER), eq(TransactionStatus.APPROVED),
                types.capture(), eq(day.atStartOfDay()), eq(day.plusDays(1).atStartOfDay()));
        assertEquals(3, types.getValue().size());
        assertEquals(false, types.getValue().contains(TransactionType.DEPOSIT));
    }
}
