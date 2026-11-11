package br.com.beca.transactions.domain.model;

/**
 * Resultado da análise de uma transação: aprovada, ou rejeitada com o motivo.
 */
public record ApprovalDecision(TransactionStatus status, String rejectionReason) {

    public static ApprovalDecision approved() {
        return new ApprovalDecision(TransactionStatus.APPROVED, null);
    }

    public static ApprovalDecision rejected(String reason) {
        return new ApprovalDecision(TransactionStatus.REJECTED, reason);
    }

    public boolean isApproved() {
        return status == TransactionStatus.APPROVED;
    }
}
