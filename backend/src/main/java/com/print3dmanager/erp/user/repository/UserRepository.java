package com.print3dmanager.erp.user.repository;

import com.print3dmanager.erp.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * Busca case-insensitive: usuarios.email é único por LOWER(email)
     * desde a V14 (índice uk_usuarios_email_lower) — o login e as
     * checagens de duplicidade precisam seguir a mesma regra.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
