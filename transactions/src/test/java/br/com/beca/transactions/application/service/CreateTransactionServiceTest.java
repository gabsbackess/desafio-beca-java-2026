package br.com.beca.transactions.application.service;

import br.com.beca.transactions.support.Ids;
import br.com.beca.transactions.application.port.in.CreateTransactionCommand;
import br.com.beca.transactions.application.port.out.AccountLimitsPort;
import br.com.beca.transactions.application.port.out.UserLookupPort;
import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.exception.AccountNotFoundException;
import br.com.beca.transactions.domain.exception.AccountServiceUnavailableException;
import br.com.beca.transactions.domain.exception.InvalidTransactionException;
import br.com.beca.transactions.domain.exception.UserServiceUnavailableException;
import br.com.beca.transactions.domain.model.AccountLimits;
import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionPolicy;
import br.com.beca.transactions.domain.model.TransactionStatus;
import br.com.beca.transactions.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTransactionServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 9, 0);

    @Mock
    private TransactionRepositoryPort transactionRepositoryPort;

    @Mock
    private AccountLimitsPort accountLimitsPort;

    @Mock
    private UserLookupPort userLookupPort;

    private CreateTransactionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"),
                ZoneId.of("America/Sao_Paulo"));
        service = new CreateTransactionService(transactionRepositoryPort, accountLimitsPort, userLookupPort, clock);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private void saveReturnsArgument() {
        when(transactionRepositoryPort.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Transaction savedTransaction() {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort, times(1)).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void shouldApproveDepositAndCreditTheBalanceWithoutCheckingLimits() {
        saveReturnsArgument();

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.DEPOSIT, bd("5000"), null, "salario", null));

        assertEquals(TransactionStatus.APPROVED, result.getStatus());
        assertNull(result.getRejectionReason());
        verify(accountLimitsPort).adjustBalance(eq(Ids.USER), argThat(delta -> delta.compareTo(bd("5000")) == 0));
        verify(accountLimitsPort, never()).findByUserId(any());
        verify(transactionRepositoryPort, never()).sumApprovedExpensesOn(any(), any());
    }

    @Test
    void shouldApproveWithdrawalWithinLimitsAndRecordCreationTime() {
        saveReturnsArgument();
        when(accountLimitsPort.findByUserId(Ids.USER))
                .thenReturn(new AccountLimits(bd("1000"), bd("500"), bd("800")));
        when(transactionRepositoryPort.sumApprovedExpensesOn(Ids.USER, TODAY)).thenReturn(BigDecimal.ZERO);

        service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.WITHDRAWAL, bd("100"), null, "saque", null));

        Transaction saved = savedTransaction();
        assertEquals(TransactionStatus.APPROVED, saved.getStatus());
        assertEquals(NOW, saved.getCreatedAt());
        assertEquals(Ids.USER, saved.getUserId());
        assertEquals(bd("100.00"), saved.getAmount());
    }

    @Test
    void shouldRejectWithdrawalAboveBalanceAndStoreReason() {
        saveReturnsArgument();
        when(accountLimitsPort.findByUserId(Ids.USER))
                .thenReturn(new AccountLimits(bd("100"), bd("1000"), bd("1000")));
        when(transactionRepositoryPort.sumApprovedExpensesOn(Ids.USER, TODAY)).thenReturn(BigDecimal.ZERO);

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.WITHDRAWAL, bd("150"), null, "saque", null));

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertEquals(TransactionPolicy.INSUFFICIENT_BALANCE, result.getRejectionReason());
        assertEquals(TransactionStatus.REJECTED, savedTransaction().getStatus());
    }

    @Test
    void shouldRejectPurchaseAbovePerTransactionLimit() {
        saveReturnsArgument();
        when(accountLimitsPort.findByUserId(Ids.USER))
                .thenReturn(new AccountLimits(bd("1000"), bd("500"), bd("2000")));
        when(transactionRepositoryPort.sumApprovedExpensesOn(Ids.USER, TODAY)).thenReturn(BigDecimal.ZERO);

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.PURCHASE, bd("600"), Category.FOOD, "mercado", null));

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertEquals(TransactionPolicy.PER_TRANSACTION_LIMIT_EXCEEDED, result.getRejectionReason());
    }

    @Test
    void shouldRejectWhenDailyLimitIsExceededByTodaysApprovedExpenses() {
        saveReturnsArgument();
        when(userLookupPort.exists(Ids.OTHER_USER)).thenReturn(true);
        when(accountLimitsPort.findByUserId(Ids.USER))
                .thenReturn(new AccountLimits(bd("1000"), bd("500"), bd("800")));
        when(transactionRepositoryPort.sumApprovedExpensesOn(Ids.USER, TODAY)).thenReturn(bd("700"));

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.TRANSFER, bd("150"), null, "pix", Ids.OTHER_USER));

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertEquals(TransactionPolicy.DAILY_LIMIT_EXCEEDED, result.getRejectionReason());
        assertEquals(Ids.OTHER_USER, result.getDestinationUserId());
    }

    @Test
    void shouldRejectAndRecordWhenAccountServiceIsUnavailable() {
        saveReturnsArgument();
        when(accountLimitsPort.findByUserId(Ids.USER))
                .thenThrow(new AccountServiceUnavailableException("fora do ar"));

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.PURCHASE, bd("50"), Category.LEISURE, "cinema", null));

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertEquals(CreateTransactionService.ACCOUNT_SERVICE_UNAVAILABLE,
                result.getRejectionReason());
        assertEquals(TransactionStatus.REJECTED, savedTransaction().getStatus());
    }

    @Test
    void shouldRejectAndRecordWhenUserHasNoAccountInTheAccountService() {
        saveReturnsArgument();
        when(accountLimitsPort.findByUserId(Ids.USER))
                .thenThrow(new AccountNotFoundException("sem conta"));

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.WITHDRAWAL, bd("50"), null, "saque", null));

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertEquals(CreateTransactionService.ACCOUNT_NOT_FOUND, result.getRejectionReason());
    }

    @Test
    void shouldNotSaveWhenRequestIsInvalid() {
        CreateTransactionCommand invalid = new CreateTransactionCommand(
                Ids.USER, TransactionType.TRANSFER, bd("10"), null, "sem destino", null);

        assertThrows(InvalidTransactionException.class, () -> service.create(invalid));

        verify(transactionRepositoryPort, never()).save(any(Transaction.class));
        verifyNoInteractions(accountLimitsPort);
    }

    @Test
    void shouldReturnTransactionSavedByRepository() {
        Transaction persisted = new Transaction(
                Ids.TX_ID, Ids.USER, null, TransactionType.DEPOSIT, bd("10.00"), Category.OTHER, "",
                TransactionStatus.APPROVED, null, NOW);
        when(transactionRepositoryPort.save(any(Transaction.class))).thenReturn(persisted);

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.DEPOSIT, bd("10"), null, null, null));

        assertEquals(Ids.TX_ID, result.getId());
    }

    @Test
    void shouldDebitTheAccountWhenAnOutflowIsApproved() {
        saveReturnsArgument();
        when(accountLimitsPort.findByUserId(Ids.USER))
                .thenReturn(new AccountLimits(bd("1000"), bd("500"), bd("800")));
        when(transactionRepositoryPort.sumApprovedExpensesOn(Ids.USER, TODAY)).thenReturn(BigDecimal.ZERO);

        service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.PURCHASE, bd("120.50"), Category.FOOD, "mercado", null));

        verify(accountLimitsPort).adjustBalance(eq(Ids.USER), argThat(delta -> delta.compareTo(bd("-120.50")) == 0));
    }

    @Test
    void shouldNotTouchTheAccountWhenTheTransactionIsRejected() {
        saveReturnsArgument();
        when(accountLimitsPort.findByUserId(Ids.USER))
                .thenReturn(new AccountLimits(bd("100"), bd("1000"), bd("1000")));
        when(transactionRepositoryPort.sumApprovedExpensesOn(Ids.USER, TODAY)).thenReturn(BigDecimal.ZERO);

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.WITHDRAWAL, bd("150"), null, "saque", null));

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        verify(accountLimitsPort, never()).adjustBalance(any(), any());
    }

    @Test
    void shouldRejectAndRecordWhenTheBalanceUpdateFails() {
        saveReturnsArgument();
        when(accountLimitsPort.findByUserId(Ids.USER))
                .thenReturn(new AccountLimits(bd("1000"), bd("500"), bd("800")));
        when(transactionRepositoryPort.sumApprovedExpensesOn(Ids.USER, TODAY)).thenReturn(BigDecimal.ZERO);
        doThrow(new AccountServiceUnavailableException("fora do ar"))
                .when(accountLimitsPort).adjustBalance(any(), any());

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.PURCHASE, bd("50"), Category.LEISURE, "cinema", null));

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertEquals(CreateTransactionService.ACCOUNT_SERVICE_UNAVAILABLE, result.getRejectionReason());
        assertEquals(TransactionStatus.REJECTED, savedTransaction().getStatus());
    }

    @Test
    void shouldRejectDepositWhenTheUserHasNoAccount() {
        saveReturnsArgument();
        doThrow(new AccountNotFoundException("sem conta"))
                .when(accountLimitsPort).adjustBalance(any(), any());

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.DEPOSIT, bd("10"), null, "salario", null));

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertEquals(CreateTransactionService.ACCOUNT_NOT_FOUND, result.getRejectionReason());
    }

    @Test
    void shouldApproveTransferToAnExistingUserAndDebitTheSender() {
        saveReturnsArgument();
        when(userLookupPort.exists(Ids.OTHER_USER)).thenReturn(true);
        when(accountLimitsPort.findByUserId(Ids.USER))
                .thenReturn(new AccountLimits(bd("1000"), bd("500"), bd("800")));
        when(transactionRepositoryPort.sumApprovedExpensesOn(Ids.USER, TODAY)).thenReturn(BigDecimal.ZERO);

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.TRANSFER, bd("200"), null, "pix", Ids.OTHER_USER));

        assertEquals(TransactionStatus.APPROVED, result.getStatus());
        verify(accountLimitsPort).adjustBalance(eq(Ids.USER), argThat(delta -> delta.compareTo(bd("-200")) == 0));
    }

    @Test
    void shouldRejectTransferWhenTheDestinationUserDoesNotExist() {
        saveReturnsArgument();
        when(userLookupPort.exists(Ids.UNKNOWN)).thenReturn(false);

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.TRANSFER, bd("200"), null, "pix", Ids.UNKNOWN));

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertEquals(CreateTransactionService.DESTINATION_NOT_FOUND, result.getRejectionReason());
        verifyNoInteractions(accountLimitsPort);
    }

    @Test
    void shouldRejectTransferWhenTheUsersServiceIsDown() {
        saveReturnsArgument();
        when(userLookupPort.exists(Ids.OTHER_USER)).thenThrow(new UserServiceUnavailableException("fora do ar"));

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.TRANSFER, bd("200"), null, "pix", Ids.OTHER_USER));

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertEquals(CreateTransactionService.DESTINATION_CHECK_UNAVAILABLE, result.getRejectionReason());
        verifyNoInteractions(accountLimitsPort);
    }

    @Test
    void shouldNotConsultTheUsersServiceForPurchases() {
        saveReturnsArgument();
        when(accountLimitsPort.findByUserId(Ids.USER))
                .thenReturn(new AccountLimits(bd("1000"), bd("500"), bd("800")));
        when(transactionRepositoryPort.sumApprovedExpensesOn(Ids.USER, TODAY)).thenReturn(BigDecimal.ZERO);

        service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.PURCHASE, bd("10"), Category.OTHER, "balas", null));

        verifyNoInteractions(userLookupPort);
    }
}
