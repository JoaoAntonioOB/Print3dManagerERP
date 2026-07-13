package com.print3dmanager.erp.user.repository;

import com.print3dmanager.erp.user.model.Role;
import com.print3dmanager.erp.user.model.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinâmicos da listagem de usuários — parâmetros nulos são
 * ignorados (padrão de filtro opcional adotado em todas as listagens).
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> comFiltros(String busca, Role role, Boolean ativo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nome")), termo),
                        cb.like(cb.lower(root.get("email")), termo)));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (ativo != null) {
                predicates.add(cb.equal(root.get("ativo"), ativo));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
