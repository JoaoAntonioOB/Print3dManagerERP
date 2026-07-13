package com.print3dmanager.erp.printer.repository;

import com.print3dmanager.erp.printer.model.Printer;
import com.print3dmanager.erp.printer.model.PrinterStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinâmicos da listagem de impressoras — parâmetros nulos são ignorados.
 */
public final class PrinterSpecifications {

    private PrinterSpecifications() {
    }

    public static Specification<Printer> comFiltros(String busca, PrinterStatus status,
                                                    Boolean ativo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nome")), termo),
                        cb.like(cb.lower(root.get("marca")), termo),
                        cb.like(cb.lower(root.get("modelo")), termo)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (ativo != null) {
                predicates.add(cb.equal(root.get("ativo"), ativo));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
