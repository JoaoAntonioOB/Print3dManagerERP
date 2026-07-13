package com.print3dmanager.erp.quote.repository;

import com.print3dmanager.erp.quote.model.Quote;
import com.print3dmanager.erp.quote.model.QuoteStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinâmicos da listagem de orçamentos — parâmetros nulos são ignorados.
 */
public final class QuoteSpecifications {

    private QuoteSpecifications() {
    }

    public static Specification<Quote> comFiltros(String busca, QuoteStatus status,
                                                  Long clienteId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("numero")), termo),
                        cb.like(cb.lower(root.join("cliente", JoinType.LEFT).get("nome")),
                                termo)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (clienteId != null) {
                predicates.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
