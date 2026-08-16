package com.print3dmanager.erp.filament;

import com.fasterxml.jackson.databind.JsonNode;
import com.print3dmanager.erp.testsupport.AbstractApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filamentos via HTTP contra o banco real: cadastro com defaults (diâmetro
 * 1.75, estoque 0), movimentação de estoque (ENTRADA/SAIDA, saldo
 * insuficiente rejeitado), soft delete, e as fronteiras de autorização —
 * ADMINISTRADOR/OPERADOR gerenciam, FINANCEIRO/VISUALIZADOR só consultam
 * (papéis diferentes do módulo Financeiro, de propósito).
 */
class FilamentFlowIntegrationTest extends AbstractApiIntegrationTest {

    private String admin;

    @BeforeEach
    void autenticar() throws Exception {
        admin = loginAdmin();
    }

    @Test
    @DisplayName("criar: defaults aplicados quando omitidos (diâmetro 1.75, estoque 0)")
    void criarAplicaDefaults() throws Exception {
        mockMvc.perform(post("/filaments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"PLA Branco","material":"PLA","custoPorKg":90.00}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diametroMm").value(1.75))
                .andExpect(jsonPath("$.quantidadeEstoqueG").value(0))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("fluxo completo: movimentar estoque (ENTRADA/SAIDA) → saldo insuficiente → desativar")
    void fluxoCompletoDeMovimentacao() throws Exception {
        long id = criarFilamento("PLA Preto Fluxo");

        mockMvc.perform(patch("/filaments/{id}/estoque", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ENTRADA\",\"quantidadeG\":500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEstoqueG").value(500));

        mockMvc.perform(patch("/filaments/{id}/estoque", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"SAIDA\",\"quantidadeG\":200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEstoqueG").value(300));

        mockMvc.perform(patch("/filaments/{id}/estoque", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"SAIDA\",\"quantidadeG\":9999}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/filaments/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/filaments/{id}/estoque", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ENTRADA\",\"quantidadeG\":10}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/filaments/{id}/ativar", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("autorização: OPERADOR gerencia filamentos (papel diferente do financeiro)")
    void operadorGerenciaFilamentos() throws Exception {
        String operador = criarUsuarioEAutenticar("OPERADOR");

        mockMvc.perform(post("/filaments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"PETG Operador","material":"PETG","custoPorKg":95.00}"""))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("autorização: FINANCEIRO consulta mas não gerencia filamentos (403)")
    void financeiroConsultaMasNaoGerenciaFilamentos() throws Exception {
        String financeiro = criarUsuarioEAutenticar("FINANCEIRO");

        mockMvc.perform(get("/filaments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(financeiro)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/filaments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(financeiro))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"ABS Financeiro","material":"ABS","custoPorKg":100.00}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("autorização: VISUALIZADOR consulta mas não gerencia filamentos (403)")
    void visualizadorConsultaMasNaoGerenciaFilamentos() throws Exception {
        String visualizador = criarUsuarioEAutenticar("VISUALIZADOR");
        long id = criarFilamento("PLA Visualizador");

        mockMvc.perform(get("/filaments/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(visualizador)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/filaments/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(visualizador)))
                .andExpect(status().isForbidden());
    }

    // ===== helpers =====

    private long criarFilamento(String nome) throws Exception {
        JsonNode criado = json(mockMvc.perform(post("/filaments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"nome\":\"%s\",\"material\":\"PLA\",\"custoPorKg\":80.00}")
                                .formatted(nome)))
                .andExpect(status().isCreated())
                .andReturn());
        return criado.get("id").asLong();
    }

    private String criarUsuarioEAutenticar(String role) throws Exception {
        String email = "%s.filament.%d@print3d.com".formatted(role.toLowerCase(), System.nanoTime());
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
