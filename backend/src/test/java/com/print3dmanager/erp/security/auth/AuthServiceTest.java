package com.print3dmanager.erp.security.auth;

import com.print3dmanager.erp.security.auth.dto.AuthResponse;
import com.print3dmanager.erp.security.auth.dto.RefreshTokenRequest;
import com.print3dmanager.erp.security.jwt.JwtService;
import com.print3dmanager.erp.user.model.Role;
import com.print3dmanager.erp.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService service;

    @Test
    @DisplayName("refresh: rotação — o token usado é revogado e um novo par é emitido")
    void refreshRotacionaToken() {
        RefreshToken atual = tokenValido(usuarioAtivo());
        when(refreshTokenRepository.findByToken(atual.getToken()))
                .thenReturn(Optional.of(atual));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.gerarAccessToken(atual.getUsuario())).thenReturn("novo-jwt");
        when(jwtService.refreshTokenValidadeMillis()).thenReturn(604_800_000L);
        when(jwtService.accessTokenValidadeSegundos()).thenReturn(900L);

        AuthResponse resposta = service.refresh(
                new RefreshTokenRequest(atual.getToken().toString()));

        assertThat(atual.isRevogado()).isTrue();
        assertThat(resposta.accessToken()).isEqualTo("novo-jwt");
        assertThat(resposta.refreshToken()).isNotEqualTo(atual.getToken().toString());
        assertThat(resposta.tokenType()).isEqualTo("Bearer");
        assertThat(resposta.expiresIn()).isEqualTo(900L);
        assertThat(resposta.usuario().email()).isEqualTo("admin@print3d.com");
    }

    @Test
    @DisplayName("refresh: reuso de token já revogado é rejeitado (detecção de replay)")
    void refreshRejeitaTokenRevogado() {
        RefreshToken revogado = tokenValido(usuarioAtivo());
        revogado.setRevogado(true);
        when(refreshTokenRepository.findByToken(revogado.getToken()))
                .thenReturn(Optional.of(revogado));

        assertThatThrownBy(() -> service.refresh(
                new RefreshTokenRequest(revogado.getToken().toString())))
                .isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("refresh: token expirado é rejeitado")
    void refreshRejeitaTokenExpirado() {
        RefreshToken expirado = tokenValido(usuarioAtivo());
        expirado.setExpiraEm(Instant.now().minus(1, ChronoUnit.HOURS));
        when(refreshTokenRepository.findByToken(expirado.getToken()))
                .thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> service.refresh(
                new RefreshTokenRequest(expirado.getToken().toString())))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("refresh: token desconhecido é rejeitado")
    void refreshRejeitaTokenDesconhecido() {
        when(refreshTokenRepository.findByToken(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(
                new RefreshTokenRequest(UUID.randomUUID().toString())))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("refresh: valor que não é UUID é rejeitado sem consultar o banco")
    void refreshRejeitaFormatoInvalido() {
        assertThatThrownBy(() -> service.refresh(new RefreshTokenRequest("nao-e-um-uuid")))
                .isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository, never()).findByToken(any());
    }

    @Test
    @DisplayName("refresh: usuário desativado não renova a sessão")
    void refreshRejeitaUsuarioInativo() {
        User inativo = usuarioAtivo();
        inativo.setAtivo(false);
        RefreshToken token = tokenValido(inativo);
        when(refreshTokenRepository.findByToken(token.getToken()))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.refresh(
                new RefreshTokenRequest(token.getToken().toString())))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    @DisplayName("logout: revoga o refresh token informado")
    void logoutRevogaToken() {
        RefreshToken token = tokenValido(usuarioAtivo());
        when(refreshTokenRepository.findByToken(token.getToken()))
                .thenReturn(Optional.of(token));

        service.logout(new RefreshTokenRequest(token.getToken().toString()));

        assertThat(token.isRevogado()).isTrue();
    }

    @Test
    @DisplayName("logout: idempotente — token desconhecido não gera erro")
    void logoutIdempotente() {
        when(refreshTokenRepository.findByToken(any(UUID.class))).thenReturn(Optional.empty());

        service.logout(new RefreshTokenRequest(UUID.randomUUID().toString()));
    }

    private User usuarioAtivo() {
        User usuario = new User();
        usuario.setId(1L);
        usuario.setNome("Administrador");
        usuario.setEmail("admin@print3d.com");
        usuario.setRole(Role.ADMINISTRADOR);
        usuario.setAtivo(true);
        return usuario;
    }

    private RefreshToken tokenValido(User usuario) {
        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setExpiraEm(Instant.now().plus(7, ChronoUnit.DAYS));
        return token;
    }
}
