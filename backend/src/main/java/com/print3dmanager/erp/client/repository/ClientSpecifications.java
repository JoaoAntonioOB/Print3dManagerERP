package com.print3dmanager.erp.client.repository;

import com.print3dmanager.erp.client.model.Client;
import com.print3dmanager.erp.client.model.PersonType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinâmicos da listagem de clientes — parâmetros nulos são ignorados.
 */
public final class ClientSpecifications {

    private ClientSpecifications() {
    }

    public static Specification<Client> comFiltros(String busca, PersonType tipoPessoa,
                                                   Boolean ativo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nome")), termo),
                        cb.like(cb.lower(root.get("email")), termo),
                        cb.like(cb.lower(root.get("cpfCnpj")), termo)));
            }
            if (tipoPessoa != null) {
                predicates.add(cb.equal(root.get("tipoPessoa"), tipoPessoa));
            }
            if (ativo != null) {
                predicates.add(cb.equal(root.get("ativo"), ativo));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
