package com.print3dmanager.erp.security.auth;

import com.print3dmanager.erp.security.auth.dto.AuthResponse;
import com.print3dmanager.erp.security.auth.dto.LoginRequest;
import com.print3dmanager.erp.security.auth.dto.RefreshTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login, renovação de tokens e logout")
@SecurityRequirements // rotas públicas — sem cadeado no Swagger
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Autentica com e-mail e senha",
            description = "Retorna o access token JWT (15 min) e o refresh token (7 dias).")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova os tokens",
            description = "Usa o refresh token para emitir um novo par de tokens. "
                    + "O refresh token informado é revogado (rotação).")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Encerra a sessão",
            description = "Revoga o refresh token informado. O access token expira sozinho.")
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
    }
}
