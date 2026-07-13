package com.print3dmanager.erp.security.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(UUID token);

    /** Revoga todas as sessões ativas do usuário (troca de senha, desativação). */
    @Modifying
    @Query("""
            update RefreshToken r
               set r.revogado = true
             where r.usuario.id = :usuarioId
               and r.revogado = false
            """)
    int revogarTodosDoUsuario(@Param("usuarioId") Long usuarioId);
}
