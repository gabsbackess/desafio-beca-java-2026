package br.com.beca.transactions.application.service;

import br.com.beca.transactions.application.port.in.CreateTransactionCommand;
import br.com.beca.transactions.application.port.in.CreateTransactionUseCase;
import br.com.beca.transactions.application.port.out.AccountLimitsPort;
import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.application.port.out.UserLookupPort;
import br.com.beca.transactions.domain.exception.AccountNotFoundException;
import br.com.beca.transactions.domain.exception.AccountServiceUnavailableException;
import br.com.beca.transactions.domain.exception.UserServiceUnavailableException;
import br.com.beca.transactions.domain.model.AccountLimits;
import br.com.beca.transactions.domain.model.ApprovalDecision;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionPolicy;
import br.com.beca.transactions.domain.model.TransactionRequest;
import br.com.beca.transactions.domain.model.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

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

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountLimitsPort accountLimitsPort;
    private final UserLookupPort userLookupPort;
    private final Clock clock;
    private final TransactionPolicy policy = new TransactionPolicy();

    public CreateTransactionService(TransactionRepositoryPort transactionRepositoryPort,
                                    AccountLimitsPort accountLimitsPort,
                                    UserLookupPort userLookupPort,
                                    Clock clock) {
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.accountLimitsPort = accountLimitsPort;
        this.userLookupPort = userLookupPort;
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
        ApprovalDecision decision = decide(request, now.toLocalDate());
        if (decision.isApproved()) {
            // Só depois de aprovar o saldo da conta é atualizado na API Mock.
            decision = applyBalanceChange(request);
        }

        return transactionRepositoryPort.save(Transaction.from(request, decision, now));
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
        BigDecimal delta = request.type().isOutflow() ? request.amount().negate() : request.amount();
        try {
            accountLimitsPort.adjustBalance(request.userId(), delta);
            return ApprovalDecision.approved();
        } catch (AccountNotFoundException exception) {
            return ApprovalDecision.rejected(ACCOUNT_NOT_FOUND);
        } catch (AccountServiceUnavailableException exception) {
            return ApprovalDecision.rejected(ACCOUNT_SERVICE_UNAVAILABLE);
        }
    }
}
