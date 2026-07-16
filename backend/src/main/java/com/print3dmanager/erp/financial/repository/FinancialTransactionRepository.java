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
}
