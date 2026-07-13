package com.print3dmanager.erp.dashboard.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Consultas de agregação do painel — SQL nativo sobre as tabelas dos
 * módulos, concentrado aqui para o dashboard permanecer autocontido.
 * Meses agrupados como YYYY-MM (to_char em UTC, o fuso do banco).
 */
@Repository
public class DashboardQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /** Pares (status, quantidade) de uma tabela com coluna status. */
    @SuppressWarnings("unchecked")
    public List<Object[]> contarPorStatus(String tabela) {
        return entityManager.createNativeQuery(
                        "SELECT status, count(*) FROM " + tabela + " GROUP BY status")
                .getResultList();
    }

    public long contarFilamentosEstoqueBaixo() {
        return contar("SELECT count(*) FROM filamentos "
                + "WHERE ativo AND quantidade_estoque_g <= estoque_minimo_g");
    }

    public long contarItensEstoqueBaixo() {
        return contar("SELECT count(*) FROM itens_estoque "
                + "WHERE ativo AND quantidade <= quantidade_minima");
    }

    public long contarClientesAtivos() {
        return contar("SELECT count(*) FROM clientes WHERE ativo");
    }

    public long contarImpressoesEmAndamento() {
        return contar("SELECT count(*) FROM historico_impressoes "
                + "WHERE status = 'EM_ANDAMENTO'");
    }

    public long contarPedidosDesde(Instant inicio) {
        Object resultado = entityManager.createNativeQuery(
                        "SELECT count(*) FROM pedidos WHERE criado_em >= :inicio")
                .setParameter("inicio", inicio)
                .getSingleResult();
        return ((Number) resultado).longValue();
    }

    public BigDecimal somarFaturamentoEntregueDesde(LocalDate inicio) {
        Object resultado = entityManager.createNativeQuery(
                        "SELECT coalesce(sum(valor_total), 0) FROM pedidos "
                                + "WHERE status = 'ENTREGUE' AND data_entrega_realizada >= :inicio")
                .setParameter("inicio", inicio)
                .getSingleResult();
        return new BigDecimal(resultado.toString());
    }

    /** Pares (YYYY-MM, quantidade) de pedidos abertos por mês. */
    @SuppressWarnings("unchecked")
    public List<Object[]> pedidosPorMes(Instant inicio) {
        return entityManager.createNativeQuery(
                        "SELECT to_char(criado_em, 'YYYY-MM'), count(*) FROM pedidos "
                                + "WHERE criado_em >= :inicio GROUP BY 1")
                .setParameter("inicio", inicio)
                .getResultList();
    }

    /** Pares (YYYY-MM, soma valor_total) de pedidos entregues por mês de entrega. */
    @SuppressWarnings("unchecked")
    public List<Object[]> faturamentoPorMes(LocalDate inicio) {
        return entityManager.createNativeQuery(
                        "SELECT to_char(data_entrega_realizada, 'YYYY-MM'), sum(valor_total) "
                                + "FROM pedidos WHERE status = 'ENTREGUE' "
                                + "AND data_entrega_realizada >= :inicio GROUP BY 1")
                .setParameter("inicio", inicio)
                .getResultList();
    }

    /** Pares (YYYY-MM, gramas) consumidas em impressões finalizadas por mês. */
    @SuppressWarnings("unchecked")
    public List<Object[]> consumoFilamentoPorMes(Instant inicio) {
        return entityManager.createNativeQuery(
                        "SELECT to_char(finalizado_em, 'YYYY-MM'), sum(peso_utilizado_g) "
                                + "FROM historico_impressoes "
                                + "WHERE peso_utilizado_g IS NOT NULL "
                                + "AND finalizado_em >= :inicio GROUP BY 1")
                .setParameter("inicio", inicio)
                .getResultList();
    }

    /** Linhas (id, nome, pedidos, valor total) dos maiores clientes. */
    @SuppressWarnings("unchecked")
    public List<Object[]> topClientes(int limite) {
        return entityManager.createNativeQuery(
                        "SELECT c.id, c.nome, count(p.id), sum(p.valor_total) "
                                + "FROM pedidos p JOIN clientes c ON c.id = p.cliente_id "
                                + "WHERE p.status <> 'CANCELADO' "
                                + "GROUP BY c.id, c.nome "
                                + "ORDER BY sum(p.valor_total) DESC, c.nome "
                                + "LIMIT :limite")
                .setParameter("limite", limite)
                .getResultList();
    }

    private long contar(String sql) {
        Object resultado = entityManager.createNativeQuery(sql).getSingleResult();
        return ((Number) resultado).longValue();
    }
}
