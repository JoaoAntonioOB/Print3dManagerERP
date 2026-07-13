package com.print3dmanager.erp.security.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de acesso")
public record LoginRequest(

        @Schema(example = "admin@print3d.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @Schema(example = "admin123")
        @NotBlank(message = "A senha é obrigatória")
        String senha
) {
}
