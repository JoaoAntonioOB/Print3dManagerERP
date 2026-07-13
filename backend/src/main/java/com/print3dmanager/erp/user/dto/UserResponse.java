package com.print3dmanager.erp.user.dto;

import com.print3dmanager.erp.user.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Dados de um usuário do sistema")
public record UserResponse(
        Long id,
        String nome,
        String email,
        Role role,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
