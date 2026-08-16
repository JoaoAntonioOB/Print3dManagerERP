package com.print3dmanager.erp.order.repository;

import com.print3dmanager.erp.order.model.PrintHistory;
import com.print3dmanager.erp.order.model.PrintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PrintHistoryRepository
        extends JpaRepository<PrintHistory, Long>, JpaSpecificationExecutor<PrintHistory> {

    /** Listagem com associações carregadas (evita N+1 nos nomes exibidos). */
    @Override
    @EntityGraph(attributePaths =
            {"impressora", "filamento", "itemPedido", "itemPedido.pedido", "usuario"})
    Page<PrintHistory> findAll(Specification<PrintHistory> spec, Pageable pageable);

    @EntityGraph(attributePaths =
            {"impressora", "filamento", "itemPedido", "itemPedido.pedido", "usuario"})
    Optional<PrintHistory> findDetalhadoById(Long id);

    /**
     * Job ativo (se houver) de uma impressora — usado para bloquear troca de
     * status/desativação enquanto há uma impressão EM_ANDAMENTO na máquina,
     * evitando "esquecer" um job cujo período deixaria de refletir a
     * realidade física.
     */
    Optional<PrintHistory> findFirstByImpressoraIdAndStatus(Long impressoraId, PrintStatus status);
}
