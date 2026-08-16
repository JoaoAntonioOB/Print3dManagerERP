package com.print3dmanager.erp.financial.repository;

import com.print3dmanager.erp.financial.model.FinancialTransaction;
import com.print3dmanager.erp.financial.model.TransactionStatus;
import com.print3dmanager.erp.financial.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransaction, Long>,
        JpaSpecificationExecutor<FinancialTransaction> {

    /** Listagem com pedido e cliente já carregados (evita N+1 na resposta). */
    @Override
    @EntityGraph(attributePaths = {"pedido", "cliente"})
    Page<FinancialTransaction> findAll(Specification<FinancialTransaction> spec,
                                       Pageable pageable);

    /** Transação completa para respostas detalhadas. */
    @EntityGraph(attributePaths = {"pedido", "cliente"})
    Optional<FinancialTransaction> findDetalhadaById(Long id);

    /** Proteção contra faturamento duplicado do mesmo pedido. */
    boolean existsByPedidoIdAndTipoAndStatusNot(Long pedidoId, TransactionType tipo,
                                                TransactionStatus status);

    /**
     * Serializa o faturamento de um mesmo pedido via advisory lock
     * transacional do Postgres — evita que duas transações concorrentes
     * (duplo clique, faturamento manual concorrendo com a geração
     * automática na entrega) passem ao mesmo tempo pelo check-then-act
     * de {@code jaFaturado()} e criem receita duplicada. Liberado
     * automaticamente no commit/rollback; complementado pela constraint
     * única parcial da migração V12 como última barreira no banco.
     */
    @Query(value = "SELECT pg_advisory_xact_lock(hashtext('pedido_faturamento_' || :pedidoId))",
            nativeQuery = true)
    void travarFaturamento(@Param("pedidoId") Long pedidoId);
}
