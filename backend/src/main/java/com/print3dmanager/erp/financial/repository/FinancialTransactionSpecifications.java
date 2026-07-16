package com.print3dmanager.erp.financial.repository;

import com.print3dmanager.erp.financial.model.FinancialTransaction;
import com.print3dmanager.erp.financial.model.TransactionStatus;
import com.print3dmanager.erp.financial.model.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinâmicos da listagem de transações — parâmetros nulos são ignorados.
 */
public final class FinancialTransactionSpecifications {

    private FinancialTransactionSpecifications() {
    }

    public static Specification<FinancialTransaction> comFiltros(String busca,
                                                                 TransactionType tipo,
                                                                 TransactionStatus status,
                                                                 String categoria,
                                                                 Long pedidoId,
                                                                 Long clienteId,
                                                                 LocalDate de,
                                                                 LocalDate ate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("descricao")), termo),
                        cb.like(cb.lower(root.get("categoria")), termo),
                        cb.like(cb.lower(root.get("observacoes")), termo)));
            }
            if (tipo != null) {
                predicates.add(cb.equal(root.get("tipo"), tipo));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (categoria != null && !categoria.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("categoria")),
                        categoria.trim().toLowerCase()));
            }
            if (pedidoId != null) {
                predicates.add(cb.equal(root.get("pedido").get("id"), pedidoId));
            }
            if (clienteId != null) {
                predicates.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (de != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataTransacao"), de));
            }
            if (ate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dataTransacao"), ate));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
