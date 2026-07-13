package com.print3dmanager.erp.security.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh token emitido no login ou no último refresh")
public record RefreshTokenRequest(

        @NotBlank(message = "O refresh token é obrigatório")
        String refreshToken
) {
}
