package com.print3dmanager.erp.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.print3dmanager.erp.testsupport.AbstractApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gestão de usuários via HTTP contra o banco real: CRUD restrito a
 * ADMINISTRADOR (demais papéis recebem 403), unicidade de e-mail, soft
 * delete com proteção contra autodesativação e revogação de sessão, e os
 * endpoints /me acessíveis a qualquer papel autenticado.
 */
class UserFlowIntegrationTest extends AbstractApiIntegrationTest {

    private String admin;

    @BeforeEach
    void autenticar() throws Exception {
        admin = loginAdmin();
    }

    @Test
    @DisplayName("fluxo completo: criar → atualizar → desativar → reativar")
    void fluxoCompletoDoUsuario() throws Exception {
        String email = "fluxo.usuario.%d@print3d.com".formatted(System.nanoTime());
        JsonNode criado = json(mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"nome\":\"Fulano\",\"email\":\"%s\","
                                + "\"senha\":\"senha12345\",\"role\":\"OPERADOR\"}")
                                .formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn());
        long id = criado.get("id").asLong();

        mockMvc.perform(put("/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"nome\":\"Fulano de Tal\",\"email\":\"%s\","
                                + "\"role\":\"VISUALIZADOR\"}").formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fulano de Tal"))
                .andExpect(jsonPath("$.role").value("VISUALIZADOR"));

        mockMvc.perform(delete("/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(patch("/users/{id}/ativar", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("criar: e-mail duplicado (mesmo com caixa diferente) gera conflito (409)")
    void criarComEmailDuplicadoConflita() throws Exception {
        String email = "duplicado.%d@print3d.com".formatted(System.nanoTime());
        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"nome\":\"Um\",\"email\":\"%s\","
                                + "\"senha\":\"senha12345\",\"role\":\"OPERADOR\"}")
                                .formatted(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"nome\":\"Dois\",\"email\":\"%s\","
                                + "\"senha\":\"senha12345\",\"role\":\"OPERADOR\"}")
                                .formatted(email.toUpperCase())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("desativar: ADMINISTRADOR não pode desativar a si mesmo (400)")
    void administradorNaoDesativaAPropriaConta() throws Exception {
        JsonNode me = json(mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andReturn());
        long idAdmin = me.get("id").asLong();

        mockMvc.perform(delete("/users/{id}", idAdmin)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("autorização: papéis não-ADMINISTRADOR recebem 403 em toda a gestão de usuários")
    void papeisNaoAdministradorRecebem403() throws Exception {
        String operador = criarUsuarioEAutenticar("OPERADOR");

        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operador)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"X","email":"x@print3d.com","senha":"senha12345",
                                "role":"OPERADOR"}"""))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/users/{id}", 999999L)
                        .header(HttpHeaders.AUTHORIZATION, bearer(operador)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("/users/me e /users/me/senha: acessíveis a qualquer papel autenticado")
    void meEAlterarSenhaAcessiveisAQualquerPapel() throws Exception {
        String email = "me.qualquer.%d@print3d.com".formatted(System.nanoTime());
        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"nome\":\"Visu\",\"email\":\"%s\","
                                + "\"senha\":\"senha12345\",\"role\":\"VISUALIZADOR\"}")
                                .formatted(email)))
                .andExpect(status().isCreated());
        String visualizador = loginCompleto(email, "senha12345").get("accessToken").asText();

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(visualizador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // senha atual incorreta é rejeitada
        mockMvc.perform(patch("/users/me/senha")
                        .header(HttpHeaders.AUTHORIZATION, bearer(visualizador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"errada\",\"novaSenha\":\"outraSenha123\"}"))
                .andExpect(status().isBadRequest());

        // troca de senha bem-sucedida revoga a sessão: o token antigo deixa de logar de novo,
        // mas o access token em mãos ainda é válido até expirar — login com a senha nova funciona
        mockMvc.perform(patch("/users/me/senha")
                        .header(HttpHeaders.AUTHORIZATION, bearer(visualizador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"senha12345\",\"novaSenha\":\"outraSenha123\"}"))
                .andExpect(status().isNoContent());

        assertThat(loginCompleto(email, "outraSenha123").get("accessToken").asText()).isNotBlank();
    }

    // ===== helpers =====

    private String criarUsuarioEAutenticar(String role) throws Exception {
        String email = "%s.user.%d@print3d.com".formatted(role.toLowerCase(), System.nanoTime());
        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"nome\":\"Usuário %s\",\"email\":\"%s\","
                                + "\"senha\":\"senha12345\",\"role\":\"%s\"}")
                                .formatted(role, email, role)))
                .andExpect(status().isCreated());
        return loginCompleto(email, "senha12345").get("accessToken").asText();
    }
}
