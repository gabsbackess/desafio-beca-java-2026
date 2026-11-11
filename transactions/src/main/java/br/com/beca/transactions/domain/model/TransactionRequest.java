package br.com.beca.transactions.domain.model;

import java.util.UUID;
import br.com.beca.transactions.domain.exception.InvalidTransactionException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pedido de transação já validado pelas regras de negócio.
 * Se o objeto existe, os dados são coerentes entre si.
 */
public record TransactionRequest(
        UUID userId,
        TransactionType type,
        BigDecimal amount,
        Category category,
        String description,
        UUID destinationUserId) {

    public TransactionRequest {
        if (userId == null) {
            throw new InvalidTransactionException("O usuário é obrigatório.");
        }
        if (type == null) {
            throw new InvalidTransactionException("O tipo da transação é obrigatório.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransactionException("O valor deve ser maior que zero.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidTransactionException("O valor deve ter no máximo duas casas decimais.");
        }
        if (type == TransactionType.TRANSFER) {
            if (destinationUserId == null) {
                throw new InvalidTransactionException("A transferência exige um usuário de destino.");
            }
            if (destinationUserId.equals(userId)) {
                throw new InvalidTransactionException("Não é possível transferir para a própria conta.");
            }
        } else if (destinationUserId != null) {
            throw new InvalidTransactionException("O usuário de destino só vale para transferências.");
        }
        if (type == TransactionType.PURCHASE && category == null) {
            throw new InvalidTransactionException("A compra exige uma categoria.");
        }

        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        if (category == null) {
            category = Category.OTHER;
        }
        description = description == null ? "" : description.trim();
    }
}
