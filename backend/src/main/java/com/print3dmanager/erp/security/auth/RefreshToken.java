package com.print3dmanager.erp.security.auth;

import com.print3dmanager.erp.common.model.BaseEntity;
import com.print3dmanager.erp.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token opaco persistido (tabela refresh_tokens — migração V10).
 * Rotação: ao ser usado, é revogado e um novo é emitido.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID token = UUID.randomUUID();

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private boolean revogado = false;

    public boolean isExpirado() {
        return Instant.now().isAfter(expiraEm);
    }

    public boolean isValido() {
        return !revogado && !isExpirado();
    }
}
