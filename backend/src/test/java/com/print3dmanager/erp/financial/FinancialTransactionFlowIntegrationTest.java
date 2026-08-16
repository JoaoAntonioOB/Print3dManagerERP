package com.print3dmanager.erp.financial;

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
 * Módulo Financeiro via HTTP contra o banco real: ciclo de vida de uma
 * transação manual (criar → editar → mudar status → excluir) e as
 * fronteiras de autorização por papel — ADMINISTRADOR/FINANCEIRO gerenciam,
 * OPERADOR/VISUALIZADOR só consultam.
 */
class FinancialTransactionFlowIntegrationTest extends AbstractApiIntegrationTest {

    private String admin;

    @BeforeEach
    void autenticar() throws Exception {
        admin = loginAdmin();
    }

    @Test
    @DisplayName("fluxo completo: lançar despesa PENDENTE → editar → pagar → estornar → excluir")
    void fluxoCompletoDaTransacao() throws Exception {
        JsonNode transacao = json(mockMvc.perform(post("/financial/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"DESPESA","categoria":"Energia elétrica",
                                "descricao":"Conta de luz","valor":150.00,
                                "dataTransacao":"2026-01-10"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn());
        long id = transacao.get("id").asLong();

        // edição em PENDENTE é permitida
        mockMvc.perform(put("/financial/transactions/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"DESPESA","categoria":"Energia elétrica",
                                "descricao":"Conta de luz (revisada)","valor":180.00,
                                "dataTransacao":"2026-01-10"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(180.00));

        // PENDENTE → PAGA
        mockMvc.perform(patch("/financial/transactions/{id}/status", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAGA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGA"));

        // edição fora de PENDENTE é rejeitada
        mockMvc.perform(put("/financial/transactions/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"DESPESA","categoria":"Energia elétrica",
                                "descricao":"Tentativa","valor":10.00,
                                "dataTransacao":"2026-01-10"}"""))
                .andExpect(status().isBadRequest());

        // exclusão fora de PENDENTE é rejeitada
        mockMvc.perform(delete("/financial/transactions/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest());

        // estorno: PAGA → PENDENTE
        mockMvc.perform(patch("/financial/transactions/{id}/status", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDENTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        // PENDENTE → CANCELADA (terminal)
        mockMvc.perform(patch("/financial/transactions/{id}/status", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        // CANCELADA é terminal: qualquer nova transição é rejeitada (400)
        mockMvc.perform(patch("/financial/transactions/{id}/status", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAGA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("criar: status CANCELADA no cadastro é rejeitado (400)")
    void criarComStatusCanceladaRejeitado() throws Exception {
        mockMvc.perform(post("/financial/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"RECEITA","categoria":"Vendas","descricao":"Teste",
                                "valor":50.00,"dataTransacao":"2026-01-10","status":"CANCELADA"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("excluir: transação PENDENTE é removida (204)")
    void excluirTransacaoPendente() throws Exception {
        JsonNode transacao = json(mockMvc.perform(post("/financial/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"RECEITA","categoria":"Vendas","descricao":"A excluir",
                                "valor":30.00,"dataTransacao":"2026-01-10"}"""))
                .andExpect(status().isCreated())
                .andReturn());
        long id = transacao.get("id").asLong();

        mockMvc.perform(delete("/financial/transactions/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/financial/transactions/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("resumo e resumo/mensal respondem 200 para quem pode consultar")
    void resumoERseumoMensalRespondem() throws Exception {
        mockMvc.perform(get("/financial/resumo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoRealizado").exists());

        mockMvc.perform(get("/financial/resumo/mensal").param("meses", "6")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    @DisplayName("autorização: OPERADOR consulta mas não gerencia transações (403 ao criar)")
    void operadorConsultaMasNaoGerencia() throws Exception {
        String operador = criarUsuarioEAutenticar("OPERADOR");

        mockMvc.perform(get("/financial/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operador)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/financial/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"RECEITA","categoria":"Vendas","descricao":"Teste",
                                "valor":50.00,"dataTransacao":"2026-01-10"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("autorização: VISUALIZADOR consulta mas não gerencia transações (403 ao excluir)")
    void visualizadorConsultaMasNaoGerencia() throws Exception {
        String visualizador = criarUsuarioEAutenticar("VISUALIZADOR");

        mockMvc.perform(get("/financial/resumo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(visualizador)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/financial/transactions/{id}", 999999L)
                        .header(HttpHeaders.AUTHORIZATION, bearer(visualizador)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("autorização: FINANCEIRO gerencia transações normalmente")
    void financeiroGerenciaTransacoes() throws Exception {
        String financeiro = criarUsuarioEAutenticar("FINANCEIRO");

        mockMvc.perform(post("/financial/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(financeiro))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"RECEITA","categoria":"Vendas","descricao":"Financeiro cria",
                                "valor":75.00,"dataTransacao":"2026-01-10"}"""))
                .andExpect(status().isCreated());
    }

    // ===== helpers =====

    /** Cria um usuário com o papel informado e devolve o access token dele. */
    private String criarUsuarioEAutenticar(String role) throws Exception {
        String email = "%s.financeiro.%d@print3d.com".formatted(role.toLowerCase(), System.nanoTime());
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
