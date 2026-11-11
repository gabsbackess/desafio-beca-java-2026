package br.com.beca.transactions.adapters.in.web.dto;

import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequest(
        @Schema(description = "DEPOSIT, WITHDRAWAL, TRANSFER ou PURCHASE", example = "PURCHASE")
        @NotNull(message = "O tipo da transação é obrigatório.")
        TransactionType type,

        @Schema(description = "Valor em reais, até 2 casas decimais", example = "89.90")
        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        @Digits(integer = 15, fraction = 2, message = "O valor deve ter no máximo duas casas decimais.")
        BigDecimal amount,

        @Schema(description = "Obrigatória na compra. FOOD, TRANSPORT, HOUSING, HEALTH, EDUCATION, LEISURE, SHOPPING ou OTHER",
                example = "FOOD")
        Category category,

        @Schema(example = "Mercado do mês")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @Schema(description = "Obrigatório na transferência", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID destinationUserId) {
}
