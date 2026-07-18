package com.print3dmanager.erp.security.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.print3dmanager.erp.testsupport.AbstractApiIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo de autenticação de ponta a ponta contra o banco real:
 * login, rotação de refresh token, logout e as respostas 401/403 em JSON.
 */
class AuthFlowIntegrationTest extends AbstractApiIntegrationTest {

    @Test
    @DisplayName("login do admin semeado devolve tokens e o resumo do usuário")
    void loginComCredenciaisValidas() throws Exception {
        JsonNode resposta = loginCompleto(ADMIN_EMAIL, ADMIN_SENHA);

        assertThat(resposta.get("accessToken").asText()).isNotBlank();
        assertThat(resposta.get("refreshToken").asText()).isNotBlank();
        assertThat(resposta.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(resposta.get("expiresIn").asLong()).isPositive();
        assertThat(resposta.get("usuario").get("email").asText()).isEqualTo(ADMIN_EMAIL);
        assertThat(resposta.get("usuario").get("role").asText()).isEqualTo("ADMINISTRADOR");
    }

    @Test
    @DisplayName("senha errada responde 401 JSON com mensagem genérica")
    void loginComSenhaErrada() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"senha-errada\"}"
                                .formatted(ADMIN_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("refresh rotaciona o token e o reuso do antigo é rejeitado")
    void refreshRotacionaEDetectaReuso() throws Exception {
        String refreshOriginal = loginCompleto(ADMIN_EMAIL, ADMIN_SENHA)
                .get("refreshToken").asText();

        JsonNode renovado = json(mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshOriginal)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(renovado.get("refreshToken").asText()).isNotEqualTo(refreshOriginal);

        // replay do token já rotacionado → 401
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshOriginal)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout revoga o refresh token da sessão")
    void logoutRevogaSessao() throws Exception {
        String refreshToken = loginCompleto(ADMIN_EMAIL, ADMIN_SENHA)
                .get("refreshToken").asText();

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rota protegida sem token responde 401 em JSON")
    void rotaProtegidaSemToken() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/users"));
    }

    @Test
    @DisplayName("token válido sem o papel exigido responde 403 em JSON")
    void perfilSemPermissaoRecebe403() throws Exception {
        String admin = loginAdmin();
        String email = "operador.auth.%d@print3d.com".formatted(System.nanoTime());
        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"nome\":\"Operador Teste\",\"email\":\"%s\","
                                + "\"senha\":\"operador123\",\"role\":\"OPERADOR\"}")
                                .formatted(email)))
                .andExpect(status().isCreated());

        String operador = loginCompleto(email, "operador123").get("accessToken").asText();

        // listagem de usuários é exclusiva do ADMINISTRADOR
        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operador)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
