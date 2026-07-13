package com.print3dmanager.erp.order.repository;

import com.print3dmanager.erp.order.model.Order;
import com.print3dmanager.erp.order.model.OrderStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinâmicos da listagem de pedidos — parâmetros nulos são ignorados.
 */
public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> comFiltros(String busca, OrderStatus status,
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
