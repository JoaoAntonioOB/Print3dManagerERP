package com.print3dmanager.erp.security.auth.dto;

import com.print3dmanager.erp.user.model.Role;
import com.print3dmanager.erp.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta de autenticação. Os campos de token seguem a convenção
 * OAuth (accessToken/refreshToken/tokenType/expiresIn); os dados do
 * usuário seguem o padrão pt-BR do restante da API.
 */
@Schema(description = "Tokens de acesso e dados do usuário autenticado")
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        @Schema(description = "Validade do access token em segundos")
        long expiresIn,
        UsuarioResumo usuario
) {

    public record UsuarioResumo(Long id, String nome, String email, Role role) {

        public static UsuarioResumo de(User usuario) {
            return new UsuarioResumo(
                    usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getRole());
        }
    }
}
