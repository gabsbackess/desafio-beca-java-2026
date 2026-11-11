package br.com.beca.transactions.application.port.out;

import java.util.UUID;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepositoryPort {

    Transaction save(Transaction transaction);

    /** Lança TransactionNotFoundException quando não existe. */
    Transaction findById(UUID id);

    /** Transações do usuário, da mais recente para a mais antiga. from, to e type são opcionais. */
    List<Transaction> findByUser(UUID userId, LocalDate from, LocalDate to, TransactionType type);

    /** Soma das saídas aprovadas (retirada, transferência e compra) do usuário no dia. */
    BigDecimal sumApprovedExpensesOn(UUID userId, LocalDate day);
}
