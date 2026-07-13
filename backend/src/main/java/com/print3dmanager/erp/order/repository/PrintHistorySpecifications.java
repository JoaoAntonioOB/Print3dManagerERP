package com.print3dmanager.erp.order.repository;

import com.print3dmanager.erp.order.model.PrintHistory;
import com.print3dmanager.erp.order.model.PrintStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinâmicos do histórico de impressões — parâmetros nulos são ignorados.
 */
public final class PrintHistorySpecifications {

    private PrintHistorySpecifications() {
    }

    public static Specification<PrintHistory> comFiltros(Long impressoraId, PrintStatus status,
                                                         Long itemPedidoId,
                                                         Instant de, Instant ate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (impressoraId != null) {
                predicates.add(cb.equal(root.get("impressora").get("id"), impressoraId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (itemPedidoId != null) {
                predicates.add(cb.equal(root.get("itemPedido").get("id"), itemPedidoId));
            }
            if (de != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("iniciadoEm"), de));
            }
            if (ate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("iniciadoEm"), ate));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
