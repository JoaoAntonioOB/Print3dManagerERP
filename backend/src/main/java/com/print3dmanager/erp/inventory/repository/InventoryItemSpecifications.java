package com.print3dmanager.erp.inventory.repository;

import com.print3dmanager.erp.inventory.model.InventoryItem;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinâmicos da listagem de itens de estoque — parâmetros nulos são ignorados.
 */
public final class InventoryItemSpecifications {

    private InventoryItemSpecifications() {
    }

    public static Specification<InventoryItem> comFiltros(String busca, String categoria,
                                                          Boolean ativo, Boolean estoqueBaixo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nome")), termo),
                        cb.like(cb.lower(root.get("descricao")), termo),
                        cb.like(cb.lower(root.get("categoria")), termo),
                        cb.like(cb.lower(root.get("localizacao")), termo)));
            }
            if (categoria != null && !categoria.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("categoria")),
                        categoria.trim().toLowerCase()));
            }
            if (ativo != null) {
                predicates.add(cb.equal(root.get("ativo"), ativo));
            }
            if (estoqueBaixo != null) {
                predicates.add(estoqueBaixo
                        ? cb.le(root.get("quantidade"), root.get("quantidadeMinima"))
                        : cb.gt(root.get("quantidade"), root.get("quantidadeMinima")));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
