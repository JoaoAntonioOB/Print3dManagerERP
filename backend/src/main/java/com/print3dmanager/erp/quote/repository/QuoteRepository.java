package com.print3dmanager.erp.quote.repository;

import com.print3dmanager.erp.quote.model.Quote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository
        extends JpaRepository<Quote, Long>, JpaSpecificationExecutor<Quote> {

    /** Listagem com associações carregadas (evita N+1 nos nomes exibidos). */
    @Override
    @EntityGraph(attributePaths = {"cliente", "usuario", "impressora", "filamento", "pedido"})
    Page<Quote> findAll(Specification<Quote> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "usuario", "impressora", "filamento", "pedido"})
    Optional<Quote> findDetalhadoById(Long id);

    /** Acesso pelo link público de aprovação. */
    @EntityGraph(attributePaths = "cliente")
    Optional<Quote> findByShareToken(UUID shareToken);

    /** Último número emitido no ano (o mais recente é o de maior id). */
    Optional<Quote> findTopByNumeroStartingWithOrderByIdDesc(String prefixo);

    /**
     * Serializa a geração de número do ano via advisory lock transacional
     * do Postgres — liberado automaticamente no commit/rollback.
     */
    @Query(value = "SELECT pg_advisory_xact_lock(hashtext('orcamentos_numero_' || :ano))",
            nativeQuery = true)
    void travarGeracaoNumero(@Param("ano") int ano);

    /**
     * Serializa a conversão de um mesmo orçamento em pedido via advisory
     * lock transacional do Postgres — evita que duas chamadas concorrentes
     * (duplo clique, retries) para o mesmo orçamento passem ao mesmo tempo
     * pelo check-then-act de status APROVADO em {@code converter()} e
     * gerem dois pedidos cobráveis. Namespace próprio (diferente do usado
     * na geração de número) para não colidir com locks de outro propósito.
     * Liberado automaticamente no commit/rollback.
     */
    @Query(value = "SELECT pg_advisory_xact_lock(hashtext('orcamento_conversao_' "
            + "|| :orcamentoId))", nativeQuery = true)
    void travarConversao(@Param("orcamentoId") Long orcamentoId);
}
