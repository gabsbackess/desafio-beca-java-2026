package br.com.beca.transactions.domain.model;

import java.math.BigDecimal;

/**
 * Regras de aprovação. Domínio puro: nada de Spring, JPA ou HTTP.
 * Depósitos são sempre aprovados. Retirada, transferência e compra passam por três regras:
 * saldo suficiente, limite por transação e limite diário.
 */
public class TransactionPolicy {

    public static final String INSUFFICIENT_BALANCE = "Saldo insuficiente.";
    public static final String PER_TRANSACTION_LIMIT_EXCEEDED = "Valor acima do limite por transação.";
    public static final String DAILY_LIMIT_EXCEEDED = "Limite diário excedido.";

    public ApprovalDecision decide(TransactionRequest request, AccountLimits limits, BigDecimal spentToday) {
        if (!request.type().isOutflow()) {
            return ApprovalDecision.approved();
        }
        BigDecimal amount = request.amount();

        if (amount.compareTo(limits.balance()) > 0) {
            return ApprovalDecision.rejected(INSUFFICIENT_BALANCE);
        }
        if (amount.compareTo(limits.perTransactionLimit()) > 0) {
            return ApprovalDecision.rejected(PER_TRANSACTION_LIMIT_EXCEEDED);
        }
        if (spentToday.add(amount).compareTo(limits.dailyLimit()) > 0) {
            return ApprovalDecision.rejected(DAILY_LIMIT_EXCEEDED);
        }
        return ApprovalDecision.approved();
    }
}
