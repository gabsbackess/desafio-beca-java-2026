package br.com.beca.transactions.adapters.out.persistence.repository;

import br.com.beca.transactions.adapters.out.persistence.entity.TransactionEntity;
import br.com.beca.transactions.domain.model.TransactionStatus;
import br.com.beca.transactions.domain.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

public interface TransactionJpaRepository
        extends JpaRepository<TransactionEntity, UUID>, JpaSpecificationExecutor<TransactionEntity> {

    /** Soma dos valores no intervalo [start, end). Devolve null quando não há linhas. */
    @Query("""
            select sum(t.amount) from TransactionEntity t
            where t.userId = :userId
              and t.status = :status
              and t.type in :types
              and t.createdAt >= :start
              and t.createdAt < :end
            """)
    BigDecimal sumAmount(@Param("userId") UUID userId,
                         @Param("status") TransactionStatus status,
                         @Param("types") Collection<TransactionType> types,
                         @Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);
}
