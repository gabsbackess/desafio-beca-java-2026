package br.com.beca.transactions.application.service;

import br.com.beca.transactions.application.port.in.CreateTransactionCommand;
import br.com.beca.transactions.application.port.in.CreateTransactionUseCase;
import br.com.beca.transactions.application.port.out.AccountLimitsPort;
import br.com.beca.transactions.application.port.out.ExchangeRatePort;
import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.application.port.out.UserLookupPort;
import br.com.beca.transactions.domain.exception.AccountNotFoundException;
import br.com.beca.transactions.domain.exception.AccountServiceUnavailableException;
import br.com.beca.transactions.domain.exception.ExchangeRateUnavailableException;
import br.com.beca.transactions.domain.exception.UserServiceUnavailableException;
import br.com.beca.transactions.domain.model.AccountLimits;
import br.com.beca.transactions.domain.model.ApprovalDecision;
import br.com.beca.transactions.domain.model.ExchangeRate;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionPolicy;
import br.com.beca.transactions.domain.model.TransactionRequest;
import br.com.beca.transactions.domain.model.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CreateTransactionService implements CreateTransactionUseCase {

    public static final String ACCOUNT_SERVICE_UNAVAILABLE =
            "Serviço de saldo indisponível no momento. Tente novamente mais tarde.";

    public static final String ACCOUNT_NOT_FOUND =
            "Conta do usuário não encontrada no serviço de saldo.";

    public static final String DESTINATION_NOT_FOUND =
            "Usuário de destino não encontrado.";

    public static final String DESTINATION_CHECK_UNAVAILABLE =
            "Serviço de usuários indisponível no momento. Tente novamente mais tarde.";

    private static final Logger log = LoggerFactory.getLogger(CreateTransactionService.class);

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountLimitsPort accountLimitsPort;
    private final UserLookupPort userLookupPort;
    private final ExchangeRatePort exchangeRatePort;
    private final Clock clock;
    private final TransactionPolicy policy = new TransactionPolicy();

    /**
     * Uma trava por usuário: duas operações do mesmo usuário não rodam ao mesmo tempo, então a leitura do saldo e a
     * atualização não se cruzam (evita duas compras simultâneas passarem do saldo). A API Mock não oferece
     * atualização condicional; a trava vale para uma instância do serviço.
     */
    private final ConcurrentHashMap<UUID, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    public CreateTransactionService(TransactionRepositoryPort transactionRepositoryPort,
                                    AccountLimitsPort accountLimitsPort,
                                    UserLookupPort userLookupPort,
                                    ExchangeRatePort exchangeRatePort,
                                    Clock clock) {
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.accountLimitsPort = accountLimitsPort;
        this.userLookupPort = userLookupPort;
        this.exchangeRatePort = exchangeRatePort;
        this.clock = clock;
    }

    @Override
    public Transaction create(CreateTransactionCommand command) {
        TransactionRequest request = new TransactionRequest(
                command.userId(),
                command.type(),
                command.amount(),
                command.category(),
                command.description(),
                command.destinationUserId());

        LocalDateTime now = LocalDateTime.now(clock);
        // A cotação é buscada antes da trava: é uma chamada externa e não depende do saldo.
        List<ExchangeRate> rates = findRates(now.toLocalDate());

        ReentrantLock lock = userLocks.computeIfAbsent(request.userId(), id -> new ReentrantLock());
        lock.lock();
        try {
            ApprovalDecision decision = decide(request, now.toLocalDate());
            boolean balanceChanged = false;
            if (decision.isApproved()) {
                // Só depois de aprovar o saldo da conta é atualizado na API Mock.
                decision = applyBalanceChange(request);
                balanceChanged = decision.isApproved();
            }

            Transaction transaction = Transaction.from(request, decision, now).withExchangeRates(rates);
            try {
                return transactionRepositoryPort.save(transaction);
            } catch (RuntimeException exception) {
                // O saldo já mudou na API Mock e a transação não foi gravada: desfaz a alteração.
                if (balanceChanged) {
                    undoBalanceChange(request);
                }
                throw exception;
            }
        } finally {
            lock.unlock();
        }
    }

    /** A taxa fica gravada na transação. Se a BrasilAPI falhar, a transação segue sem conversão. */
    private List<ExchangeRate> findRates(LocalDate day) {
        try {
            return exchangeRatePort.findRates(day);
        } catch (ExchangeRateUnavailableException exception) {
            log.warn("Câmbio indisponível: {}", exception.getMessage(), exception);
            return List.of();
        }
    }

    private ApprovalDecision decide(TransactionRequest request, LocalDate today) {
        if (request.type() == TransactionType.TRANSFER) {
            ApprovalDecision destination = checkDestination(request.destinationUserId());
            if (destination != null) {
                return destination;
            }
        }
        // Depósito não precisa consultar saldo nem limites.
        if (!request.type().isOutflow()) {
            return ApprovalDecision.approved();
        }
        try {
            AccountLimits limits = accountLimitsPort.findByUserId(request.userId());
            BigDecimal spentToday =
                    transactionRepositoryPort.sumApprovedExpensesOn(request.userId(), today);
            return policy.decide(request, limits, spentToday);
        } catch (AccountNotFoundException exception) {
            return ApprovalDecision.rejected(ACCOUNT_NOT_FOUND);
        } catch (AccountServiceUnavailableException exception) {
            // Nunca aprovar às cegas: a transação fica gravada como rejeitada, com o motivo.
            return ApprovalDecision.rejected(ACCOUNT_SERVICE_UNAVAILABLE);
        }
    }

    /** O destino precisa existir no MS1. Devolve null quando está tudo certo. */
    private ApprovalDecision checkDestination(UUID destinationUserId) {
        try {
            if (!userLookupPort.exists(destinationUserId)) {
                return ApprovalDecision.rejected(DESTINATION_NOT_FOUND);
            }
            return null;
        } catch (UserServiceUnavailableException exception) {
            return ApprovalDecision.rejected(DESTINATION_CHECK_UNAVAILABLE);
        }
    }

    /** Retirada, transferência e compra debitam; depósito credita. Se a atualização falhar, rejeita. */
    private ApprovalDecision applyBalanceChange(TransactionRequest request) {
        try {
            accountLimitsPort.adjustBalance(request.userId(), balanceDelta(request));
            return ApprovalDecision.approved();
        } catch (AccountNotFoundException exception) {
            return ApprovalDecision.rejected(ACCOUNT_NOT_FOUND);
        } catch (AccountServiceUnavailableException exception) {
            return ApprovalDecision.rejected(ACCOUNT_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Desfaz o ajuste de saldo quando a transação não pôde ser gravada. Se o próprio desfazer falhar, registra no log
     * o usuário e o valor, para correção manual.
     */
    private void undoBalanceChange(TransactionRequest request) {
        BigDecimal undo = balanceDelta(request).negate();
        try {
            accountLimitsPort.adjustBalance(request.userId(), undo);
            log.warn("Transação não gravada: saldo do usuário {} desfeito ({}).", request.userId(), undo);
        } catch (RuntimeException exception) {
            log.error("FALHA ao desfazer o saldo do usuário {} (valor a aplicar: {}). Corrigir manualmente.",
                    request.userId(), undo, exception);
        }
    }

    private static BigDecimal balanceDelta(TransactionRequest request) {
        return request.type().isOutflow() ? request.amount().negate() : request.amount();
    }
}
