package com.print3dmanager.erp.order.repository;

import com.print3dmanager.erp.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    /** Listagem com cliente já carregado (evita N+1 no resumo). */
    @Override
    @EntityGraph(attributePaths = "cliente")
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

    /** Pedido completo para respostas detalhadas. */
    @EntityGraph(attributePaths = {"cliente", "usuario", "itens", "itens.filamento"})
    Optional<Order> findDetalhadoById(Long id);

    /** Último número emitido no ano (o mais recente é o de maior id). */
    Optional<Order> findTopByNumeroStartingWithOrderByIdDesc(String prefixo);

    /**
     * Serializa a geração de número do ano via advisory lock transacional
     * do Postgres — liberado automaticamente no commit/rollback.
     */
    @Query(value = "SELECT pg_advisory_xact_lock(hashtext('pedidos_numero_' || :ano))",
            nativeQuery = true)
    void travarGeracaoNumero(@Param("ano") int ano);
}
