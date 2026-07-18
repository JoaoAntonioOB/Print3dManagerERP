package com.print3dmanager.erp.security.jwt;

import com.print3dmanager.erp.config.JwtProperties;
import com.print3dmanager.erp.user.model.Role;
import com.print3dmanager.erp.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "chave-de-teste-para-hmac-sha256-com-32+bytes".getBytes(StandardCharsets.UTF_8));

    private final JwtService jwtService =
            new JwtService(new JwtProperties(SECRET, 900_000L, 604_800_000L));

    @Test
    @DisplayName("token emitido é validado e devolve o e-mail (subject)")
    void gerarEExtrairEmail() {
        String token = jwtService.gerarAccessToken(usuario());

        assertThat(jwtService.extrairEmail(token)).contains("admin@print3d.com");
    }

    @Test
    @DisplayName("token assinado com outra chave é rejeitado sem exceção")
    void rejeitaAssinaturaDeOutraChave() {
        String outroSecret = Base64.getEncoder().encodeToString(
                "outra-chave-de-teste-para-hmac-sha256-32bytes".getBytes(StandardCharsets.UTF_8));
        JwtService outroServico =
                new JwtService(new JwtProperties(outroSecret, 900_000L, 604_800_000L));
        String tokenAlheio = outroServico.gerarAccessToken(usuario());

        assertThat(jwtService.extrairEmail(tokenAlheio)).isEmpty();
    }

    @Test
    @DisplayName("token expirado é rejeitado sem exceção")
    void rejeitaTokenExpirado() {
        JwtService servicoExpirado =
                new JwtService(new JwtProperties(SECRET, -1_000L, 604_800_000L));
        String tokenExpirado = servicoExpirado.gerarAccessToken(usuario());

        assertThat(jwtService.extrairEmail(tokenExpirado)).isEmpty();
    }

    @Test
    @DisplayName("lixo e nulos não derrubam a validação")
    void rejeitaTokenMalformado() {
        assertThat(jwtService.extrairEmail("nao-e-um-jwt")).isEmpty();
        assertThat(jwtService.extrairEmail("")).isEmpty();
        assertThat(jwtService.extrairEmail(null)).isEmpty();
    }

    @Test
    @DisplayName("validade do access token é exposta em segundos para o corpo do login")
    void validadeEmSegundos() {
        assertThat(jwtService.accessTokenValidadeSegundos()).isEqualTo(900L);
        assertThat(jwtService.refreshTokenValidadeMillis()).isEqualTo(604_800_000L);
    }

    private User usuario() {
        User usuario = new User();
        usuario.setId(1L);
        usuario.setNome("Administrador");
        usuario.setEmail("admin@print3d.com");
        usuario.setRole(Role.ADMINISTRADOR);
        return usuario;
    }
}
