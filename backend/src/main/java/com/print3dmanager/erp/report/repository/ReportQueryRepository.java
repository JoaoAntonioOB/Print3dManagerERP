package com.print3dmanager.erp.report.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Consultas dos relatórios — SQL nativo no padrão do dashboard, concentrado
 * aqui para o módulo ficar autocontido. Períodos sobre TIMESTAMPTZ chegam
 * como [inicio, fimExclusivo) em Instant UTC; sobre DATE, como LocalDate
 * inclusivo.
 */
@Repository
public class ReportQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Linhas (numero, data de abertura DD/MM/YYYY, cliente, status, entrega
     * prevista DD/MM/YYYY ou null, valor_total) dos pedidos abertos no
     * período, opcionalmente por status. Datas já formatadas no SQL (UTC).
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> pedidosDoPeriodo(Instant inicio, Instant fimExclusivo, String status) {
        String sql = "SELECT p.numero, to_char(p.criado_em, 'DD/MM/YYYY'), c.nome, p.status, "
                + "to_char(p.data_entrega_prevista, 'DD/MM/YYYY'), p.valor_total "
                + "FROM pedidos p JOIN clientes c ON c.id = p.cliente_id "
                + "WHERE p.criado_em >= :inicio AND p.criado_em < :fim ";
        if (status != null) {
            sql += "AND p.status = :status ";
        }
        sql += "ORDER BY p.criado_em";
        Query query = entityManager.createNativeQuery(sql)
                .setParameter("inicio", inicio)
                .setParameter("fim", fimExclusivo);
        if (status != null) {
            query.setParameter("status", status);
        }
        return query.getResultList();
    }

    /**
     * Linhas (data DD/MM/YYYY, tipo, categoria, descricao, cliente ou null,
     * status, valor) das transações do período, canceladas incluídas (a
     * situação aparece no relatório).
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> transacoesDoPeriodo(LocalDate de, LocalDate ate) {
        return entityManager.createNativeQuery(
                        "SELECT to_char(t.data_transacao, 'DD/MM/YYYY'), t.tipo, t.categoria, "
                                + "t.descricao, "
                                + "c.nome, t.status, t.valor "
                                + "FROM transacoes_financeiras t "
                                + "LEFT JOIN clientes c ON c.id = t.cliente_id "
                                + "WHERE t.data_transacao BETWEEN :de AND :ate "
                                + "ORDER BY t.data_transacao, t.id")
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getResultList();
    }

    /**
     * Linhas (filamento, marca, material, cor, jobs, gramas, custo) do
     * consumo agregado por filamento nas impressões finalizadas no período
     * com peso registrado, da maior para a menor quantidade consumida.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> consumoPorFilamento(Instant inicio, Instant fimExclusivo) {
        return entityManager.createNativeQuery(
                        "SELECT f.nome, f.marca, f.material, f.cor, count(h.id), "
                                + "sum(h.peso_utilizado_g), sum(h.custo_total) "
                                + "FROM historico_impressoes h "
                                + "JOIN filamentos f ON f.id = h.filamento_id "
                                + "WHERE h.peso_utilizado_g IS NOT NULL "
                                + "AND h.finalizado_em >= :inicio AND h.finalizado_em < :fim "
                                + "GROUP BY f.id, f.nome, f.marca, f.material, f.cor "
                                + "ORDER BY sum(h.peso_utilizado_g) DESC, f.nome")
                .setParameter("inicio", inicio)
                .setParameter("fim", fimExclusivo)
                .getResultList();
    }
}
