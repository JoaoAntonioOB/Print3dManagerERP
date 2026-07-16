package com.print3dmanager.erp.financial.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Agregações do resumo financeiro — SQL nativo no padrão do dashboard.
 * Meses agrupados como YYYY-MM sobre data_transacao (coluna DATE).
 */
@Repository
public class FinancialQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /** Linhas (tipo, status, soma) do período, sem transações CANCELADAS. */
    @SuppressWarnings("unchecked")
    public List<Object[]> totaisPorTipoEStatus(LocalDate de, LocalDate ate) {
        return entityManager.createNativeQuery(
                        "SELECT tipo, status, sum(valor) FROM transacoes_financeiras "
                                + "WHERE status <> 'CANCELADA' "
                                + "AND data_transacao BETWEEN :de AND :ate "
                                + "GROUP BY tipo, status")
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getResultList();
    }

    /** Linhas (YYYY-MM, tipo, soma) das transações PAGAS por mês. */
    @SuppressWarnings("unchecked")
    public List<Object[]> movimentoPagoPorMes(LocalDate inicio) {
        return entityManager.createNativeQuery(
                        "SELECT to_char(data_transacao, 'YYYY-MM'), tipo, sum(valor) "
                                + "FROM transacoes_financeiras "
                                + "WHERE status = 'PAGA' AND data_transacao >= :inicio "
                                + "GROUP BY 1, tipo")
                .setParameter("inicio", inicio)
                .getResultList();
    }
}
