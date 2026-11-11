package br.com.beca.transactions.adapters.out.persistence;

import br.com.beca.transactions.adapters.out.persistence.entity.TransactionEntity;
import br.com.beca.transactions.adapters.out.persistence.mapper.TransactionMapper;
import br.com.beca.transactions.adapters.out.persistence.repository.TransactionJpaRepository;
import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.exception.TransactionNotFoundException;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionStatus;
import br.com.beca.transactions.domain.model.TransactionType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class TransactionPersistenceAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository repository;
    private final TransactionMapper mapper;

    public TransactionPersistenceAdapter(TransactionJpaRepository repository, TransactionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Transaction save(Transaction transaction) {
        return mapper.toDomain(repository.save(mapper.toEntity(transaction)));
    }

    @Override
    public Transaction findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain)
                .orElseThrow(() -> new TransactionNotFoundException("Transação não encontrada."));
    }

    @Override
    public List<Transaction> findByUser(UUID userId, LocalDate from, LocalDate to, TransactionType type) {
        Specification<TransactionEntity> spec =
                (root, query, builder) -> builder.equal(root.get("userId"), userId);
        if (from != null) {
            LocalDateTime start = from.atStartOfDay();
            spec = spec.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), start));
        }
        if (to != null) {
            LocalDateTime end = to.plusDays(1).atStartOfDay();
            spec = spec.and((root, query, builder) -> builder.lessThan(root.get("createdAt"), end));
        }
        if (type != null) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("type"), type));
        }
        return repository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public BigDecimal sumApprovedExpensesOn(UUID userId, LocalDate day) {
        List<TransactionType> outflows = Arrays.stream(TransactionType.values())
                .filter(TransactionType::isOutflow)
                .toList();
        BigDecimal sum = repository.sumAmount(
                userId, TransactionStatus.APPROVED, outflows, day.atStartOfDay(), day.plusDays(1).atStartOfDay());
        return sum == null ? BigDecimal.ZERO : sum;
    }
}
