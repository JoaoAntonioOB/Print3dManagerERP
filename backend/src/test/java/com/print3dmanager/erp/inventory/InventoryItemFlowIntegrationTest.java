package com.print3dmanager.erp.inventory;

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
 * Itens de estoque (insumos gerais) via HTTP contra o banco real: cadastro
 * com defaults (quantidade 0, unidade UN), movimentação com saldo
 * insuficiente rejeitado, soft delete, e as mesmas fronteiras de
 * autorização do módulo de filamentos (ADMINISTRADOR/OPERADOR gerenciam;
 * FINANCEIRO/VISUALIZADOR só consultam).
 */
class InventoryItemFlowIntegrationTest extends AbstractApiIntegrationTest {

    private String admin;

    @BeforeEach
    void autenticar() throws Exception {
        admin = loginAdmin();
    }

    @Test
    @DisplayName("criar: defaults aplicados quando omitidos (quantidade 0, unidade UN)")
    void criarAplicaDefaults() throws Exception {
        mockMvc.perform(post("/inventory")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Parafuso M3x10\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidade").value(0))
                .andExpect(jsonPath("$.unidadeMedida").value("UN"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("fluxo completo: movimentar estoque → saldo insuficiente → desativar → reativar")
    void fluxoCompletoDeMovimentacao() throws Exception {
        long id = criarItem("Parafuso Fluxo");

        mockMvc.perform(patch("/inventory/{id}/estoque", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ENTRADA\",\"quantidade\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(100));

        mockMvc.perform(patch("/inventory/{id}/estoque", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"SAIDA\",\"quantidade\":500}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/inventory/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/inventory/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(patch("/inventory/{id}/ativar", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("autorização: OPERADOR gerencia itens de estoque")
    void operadorGerenciaItens() throws Exception {
        String operador = criarUsuarioEAutenticar("OPERADOR");

        mockMvc.perform(post("/inventory")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Item Operador\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("autorização: FINANCEIRO e VISUALIZADOR consultam mas não gerenciam (403)")
    void financeiroEVisualizadorNaoGerenciam() throws Exception {
        String financeiro = criarUsuarioEAutenticar("FINANCEIRO");
        String visualizador = criarUsuarioEAutenticar("VISUALIZADOR");

        mockMvc.perform(get("/inventory")
                        .header(HttpHeaders.AUTHORIZATION, bearer(financeiro)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/inventory")
                        .header(HttpHeaders.AUTHORIZATION, bearer(financeiro))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Item Financeiro\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/inventory")
                        .header(HttpHeaders.AUTHORIZATION, bearer(visualizador)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/inventory/{id}", 999999L)
                        .header(HttpHeaders.AUTHORIZATION, bearer(visualizador)))
                .andExpect(status().isForbidden());
    }

    // ===== helpers =====

    private long criarItem(String nome) throws Exception {
        JsonNode criado = json(mockMvc.perform(post("/inventory")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"%s\"}".formatted(nome)))
                .andExpect(status().isCreated())
                .andReturn());
        return criado.get("id").asLong();
    }

    private String criarUsuarioEAutenticar(String role) throws Exception {
        String email = "%s.inventory.%d@print3d.com".formatted(role.toLowerCase(), System.nanoTime());
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
