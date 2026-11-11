package br.com.beca.transactions.domain.model;

public enum TransactionType {
    DEPOSIT(false),
    WITHDRAWAL(true),
    TRANSFER(true),
    PURCHASE(true);

    private final boolean outflow;

    TransactionType(boolean outflow) {
        this.outflow = outflow;
    }

    /** Retirada, transferência enviada e compra tiram dinheiro da conta. */
    public boolean isOutflow() {
        return outflow;
    }
}
