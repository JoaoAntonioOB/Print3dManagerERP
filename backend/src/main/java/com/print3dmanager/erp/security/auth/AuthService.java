package com.print3dmanager.erp.security.auth;

import com.print3dmanager.erp.security.SecurityUser;
import com.print3dmanager.erp.security.auth.dto.AuthResponse;
import com.print3dmanager.erp.security.auth.dto.LoginRequest;
import com.print3dmanager.erp.security.auth.dto.RefreshTokenRequest;
import com.print3dmanager.erp.security.jwt.JwtService;
import com.print3dmanager.erp.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Fluxo de autenticação JWT: login, refresh com rotação e logout.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String MSG_TOKEN_INVALIDO = "Refresh token inválido ou expirado.";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));

        User usuario = ((SecurityUser) authentication.getPrincipal()).getUser();
        return gerarTokens(usuario);
    }

    /**
     * Rotação de refresh token: o token usado é revogado e um novo par
     * access/refresh é emitido. Token reutilizado ou expirado → 401.
     */
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken atual = refreshTokenRepository.findByToken(parseToken(request.refreshToken()))
                .orElseThrow(() -> new BadCredentialsException(MSG_TOKEN_INVALIDO));

        if (!atual.isValido()) {
            throw new BadCredentialsException(MSG_TOKEN_INVALIDO);
        }

        User usuario = atual.getUsuario();
        if (!usuario.isAtivo()) {
            throw new DisabledException("Usuário inativo.");
        }

        atual.setRevogado(true);
        return gerarTokens(usuario);
    }

    /** Revoga o refresh token informado. Idempotente: token desconhecido é ignorado. */
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(parseToken(request.refreshToken()))
                .ifPresent(token -> token.setRevogado(true));
    }

    private AuthResponse gerarTokens(User usuario) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setExpiraEm(Instant.now().plusMillis(jwtService.refreshTokenValidadeMillis()));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                jwtService.gerarAccessToken(usuario),
                refreshToken.getToken().toString(),
                TOKEN_TYPE,
                jwtService.accessTokenValidadeSegundos(),
                AuthResponse.UsuarioResumo.de(usuario));
    }

    private UUID parseToken(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException(MSG_TOKEN_INVALIDO);
        }
    }
}
