package com.print3dmanager.erp.filament.repository;

import com.print3dmanager.erp.filament.model.Filament;
import com.print3dmanager.erp.filament.model.FilamentMaterial;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinâmicos da listagem de filamentos — parâmetros nulos são ignorados.
 */
public final class FilamentSpecifications {

    private FilamentSpecifications() {
    }

    public static Specification<Filament> comFiltros(String busca, FilamentMaterial material,
                                                     Boolean ativo, Boolean estoqueBaixo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nome")), termo),
                        cb.like(cb.lower(root.get("marca")), termo),
                        cb.like(cb.lower(root.get("cor")), termo)));
            }
            if (material != null) {
                predicates.add(cb.equal(root.get("material"), material));
            }
            if (ativo != null) {
                predicates.add(cb.equal(root.get("ativo"), ativo));
            }
            if (estoqueBaixo != null) {
                predicates.add(estoqueBaixo
                        ? cb.le(root.get("quantidadeEstoqueG"), root.get("estoqueMinimoG"))
                        : cb.gt(root.get("quantidadeEstoqueG"), root.get("estoqueMinimoG")));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
