package br.com.beca.transactions.application.service;

import br.com.beca.transactions.support.Ids;
import br.com.beca.transactions.application.port.in.CreateTransactionCommand;
import br.com.beca.transactions.application.port.out.AccountLimitsPort;
import br.com.beca.transactions.application.port.out.UserLookupPort;
import br.com.beca.transactions.application.port.out.ExchangeRatePort;
import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.exception.AccountNotFoundException;
import br.com.beca.transactions.domain.exception.AccountServiceUnavailableException;
import br.com.beca.transactions.domain.exception.ExchangeRateUnavailableException;
import br.com.beca.transactions.domain.exception.InvalidTransactionException;
import br.com.beca.transactions.domain.exception.UserServiceUnavailableException;
import br.com.beca.transactions.domain.model.AccountLimits;
import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.Currency;
import br.com.beca.transactions.domain.model.ExchangeRate;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionPolicy;
import br.com.beca.transactions.domain.model.TransactionStatus;
import br.com.beca.transactions.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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

    @Mock
    private ExchangeRatePort exchangeRatePort;

    private CreateTransactionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"),
                ZoneId.of("America/Sao_Paulo"));
        service = new CreateTransactionService(transactionRepositoryPort, accountLimitsPort, userLookupPort,
                exchangeRatePort, clock);
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
    void shouldStoreExchangeRatesOfTheDayInTheTransaction() {
        saveReturnsArgument();
        List<ExchangeRate> rates = List.of(
                new ExchangeRate(Currency.USD, bd("5.30"), TODAY),
                new ExchangeRate(Currency.EUR, bd("6.10"), TODAY));
        when(exchangeRatePort.findRates(TODAY)).thenReturn(rates);

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.DEPOSIT, bd("100"), null, "salario", null));

        assertEquals(rates, result.getExchangeRates());
    }

    @Test
    void shouldCreateTransactionWithoutConversionWhenExchangeServiceIsDown() {
        saveReturnsArgument();
        when(exchangeRatePort.findRates(TODAY)).thenThrow(new ExchangeRateUnavailableException("fora do ar"));

        Transaction result = service.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.DEPOSIT, bd("100"), null, "salario", null));

        assertEquals(TransactionStatus.APPROVED, result.getStatus());
        assertEquals(false, result.hasExchangeRates());
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

    @Test
    void shouldUndoTheBalanceChangeWhenTheTransactionCannotBeSaved() {
        when(accountLimitsPort.findByUserId(Ids.USER))
                .thenReturn(new AccountLimits(bd("1000"), bd("500"), bd("800")));
        when(transactionRepositoryPort.sumApprovedExpensesOn(Ids.USER, TODAY)).thenReturn(BigDecimal.ZERO);
        RuntimeException failure = new RuntimeException("banco fora do ar");
        when(transactionRepositoryPort.save(any(Transaction.class))).thenThrow(failure);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.create(
                new CreateTransactionCommand(Ids.USER, TransactionType.PURCHASE, bd("100"), Category.FOOD, "jantar", null)));

        assertSame(failure, thrown);
        InOrder order = inOrder(accountLimitsPort);
        order.verify(accountLimitsPort).adjustBalance(eq(Ids.USER), argThat(delta -> delta.compareTo(bd("-100")) == 0));
        order.verify(accountLimitsPort).adjustBalance(eq(Ids.USER), argThat(delta -> delta.compareTo(bd("100")) == 0));
    }

    @Test
    void shouldUndoTheCreditWhenADepositCannotBeSaved() {
        when(transactionRepositoryPort.save(any(Transaction.class))).thenThrow(new RuntimeException("banco fora do ar"));

        assertThrows(RuntimeException.class, () -> service.create(
                new CreateTransactionCommand(Ids.USER, TransactionType.DEPOSIT, bd("300"), null, "salario", null)));

        verify(accountLimitsPort).adjustBalance(eq(Ids.USER), argThat(delta -> delta.compareTo(bd("300")) == 0));
        verify(accountLimitsPort).adjustBalance(eq(Ids.USER), argThat(delta -> delta.compareTo(bd("-300")) == 0));
    }

    @Test
    void shouldNotTouchTheBalanceWhenARejectedTransactionCannotBeSaved() {
        when(accountLimitsPort.findByUserId(Ids.USER))
                .thenReturn(new AccountLimits(bd("100"), bd("1000"), bd("1000")));
        when(transactionRepositoryPort.sumApprovedExpensesOn(Ids.USER, TODAY)).thenReturn(BigDecimal.ZERO);
        when(transactionRepositoryPort.save(any(Transaction.class))).thenThrow(new RuntimeException("banco fora do ar"));

        assertThrows(RuntimeException.class, () -> service.create(
                new CreateTransactionCommand(Ids.USER, TransactionType.WITHDRAWAL, bd("150"), null, "saque", null)));

        verify(accountLimitsPort, never()).adjustBalance(any(), any());
    }

    @Test
    void shouldKeepTheOriginalErrorWhenUndoingTheBalanceAlsoFails() {
        when(accountLimitsPort.findByUserId(Ids.USER))
                .thenReturn(new AccountLimits(bd("1000"), bd("500"), bd("800")));
        when(transactionRepositoryPort.sumApprovedExpensesOn(Ids.USER, TODAY)).thenReturn(BigDecimal.ZERO);
        RuntimeException failure = new RuntimeException("banco fora do ar");
        when(transactionRepositoryPort.save(any(Transaction.class))).thenThrow(failure);
        // 1ª chamada (o débito) funciona; 2ª chamada (o desfazer) falha.
        doNothing().doThrow(new AccountServiceUnavailableException("fora do ar"))
                .when(accountLimitsPort).adjustBalance(any(), any());

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.create(
                new CreateTransactionCommand(Ids.USER, TransactionType.PURCHASE, bd("50"), Category.FOOD, "lanche", null)));

        assertSame(failure, thrown);
    }

    /** Conta simulada que lê e grava devagar, para expor a corrida se a trava não existisse. */
    private static final class SlowAccount implements AccountLimitsPort {
        private volatile BigDecimal balance;

        private SlowAccount(BigDecimal balance) {
            this.balance = balance;
        }

        @Override
        public AccountLimits findByUserId(UUID userId) {
            AccountLimits limits = new AccountLimits(balance, bd("1000"), bd("1000"));
            pause();
            return limits;
        }

        @Override
        public void adjustBalance(UUID userId, BigDecimal delta) {
            BigDecimal current = balance;
            pause();
            balance = current.add(delta);
        }

        private static void pause() {
            try {
                Thread.sleep(50);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    void shouldNotLetTwoSimultaneousPurchasesOfTheSameUserExceedTheBalance() throws Exception {
        saveReturnsArgument();
        when(transactionRepositoryPort.sumApprovedExpensesOn(Ids.USER, TODAY)).thenReturn(BigDecimal.ZERO);
        SlowAccount account = new SlowAccount(bd("100"));
        Clock clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        CreateTransactionService concurrent = new CreateTransactionService(
                transactionRepositoryPort, account, userLookupPort, exchangeRatePort, clock);

        Callable<Transaction> purchase = () -> concurrent.create(new CreateTransactionCommand(
                Ids.USER, TransactionType.PURCHASE, bd("80"), Category.FOOD, "compra", null));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Transaction> first = pool.submit(purchase);
            Future<Transaction> second = pool.submit(purchase);
            List<TransactionStatus> statuses = List.of(first.get().getStatus(), second.get().getStatus());

            assertEquals(1, statuses.stream().filter(s -> s == TransactionStatus.APPROVED).count());
            assertEquals(1, statuses.stream().filter(s -> s == TransactionStatus.REJECTED).count());
            assertEquals(0, bd("20").compareTo(account.balance));
        } finally {
            pool.shutdownNow();
        }
    }
}
