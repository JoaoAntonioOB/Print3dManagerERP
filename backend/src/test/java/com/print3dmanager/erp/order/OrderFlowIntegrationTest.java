package com.print3dmanager.erp.order;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de vida completo do pedido via HTTP contra o banco real:
 * criação com total calculado, máquina de estados, faturamento e a
 * receita gerada no módulo Financeiro.
 */
class OrderFlowIntegrationTest extends AbstractApiIntegrationTest {

    private String admin;

    @BeforeEach
    void autenticar() throws Exception {
        admin = loginAdmin();
    }

    @Test
    @DisplayName("fluxo completo: criar → produzir → concluir → faturar → entregar sem receita duplicada")
    void fluxoCompletoDoPedido() throws Exception {
        long clienteId = criarCliente("Cliente Fluxo Pedido");
        JsonNode pedido = criarPedido(clienteId);
        long pedidoId = pedido.get("id").asLong();

        // número no padrão PED-<ano>-NNNN e total 2 × 40,00 + 1 × 20,00 − 10,00 = 90,00
        assertThat(pedido.get("numero").asText()).matches("PED-\\d{4}-\\d{4}");
        assertThat(pedido.get("valorTotal").decimalValue()).isEqualByComparingTo("90.00");
        assertThat(pedido.get("status").asText()).isEqualTo("PENDENTE");

        alterarStatus(pedidoId, "EM_PRODUCAO");
        alterarStatus(pedidoId, "CONCLUIDO");

        // faturamento manual gera receita PENDENTE vinculada
        mockMvc.perform(post("/orders/{id}/faturar", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("RECEITA"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.categoria").value("Vendas"))
                .andExpect(jsonPath("$.valor").value(90.00))
                .andExpect(jsonPath("$.pedidoId").value(pedidoId))
                .andExpect(jsonPath("$.clienteId").value(clienteId));

        // refaturar em seguida conflita
        mockMvc.perform(post("/orders/{id}/faturar", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict());

        // entrega automática NÃO duplica a receita já emitida
        alterarStatus(pedidoId, "ENTREGUE");
        mockMvc.perform(get("/financial/transactions")
                        .param("pedidoId", String.valueOf(pedidoId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // ENTREGUE grava a data de entrega realizada
        mockMvc.perform(get("/orders/{id}", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"))
                .andExpect(jsonPath("$.dataEntregaRealizada").exists());
    }

    @Test
    @DisplayName("entrega sem faturamento manual gera a receita automaticamente")
    void entregaGeraReceitaAutomatica() throws Exception {
        long clienteId = criarCliente("Cliente Receita Automática");
        long pedidoId = criarPedido(clienteId).get("id").asLong();

        alterarStatus(pedidoId, "EM_PRODUCAO");
        alterarStatus(pedidoId, "CONCLUIDO");
        alterarStatus(pedidoId, "ENTREGUE");

        mockMvc.perform(get("/financial/transactions")
                        .param("pedidoId", String.valueOf(pedidoId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value("RECEITA"))
                .andExpect(jsonPath("$.content[0].valor").value(90.00));
    }

    @Test
    @DisplayName("transição inválida de status responde 400 com a transição na mensagem")
    void transicaoInvalida() throws Exception {
        long clienteId = criarCliente("Cliente Transição Inválida");
        long pedidoId = criarPedido(clienteId).get("id").asLong();

        mockMvc.perform(patch("/orders/{id}/status", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ENTREGUE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Transição de status inválida")));
    }

    @Test
    @DisplayName("pedido fora de PENDENTE não pode ser excluído")
    void exclusaoSomentePendente() throws Exception {
        long clienteId = criarCliente("Cliente Exclusão");
        long pedidoId = criarPedido(clienteId).get("id").asLong();
        alterarStatus(pedidoId, "EM_PRODUCAO");

        mockMvc.perform(delete("/orders/{id}", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    // ===== helpers =====

    private long criarCliente(String nome) throws Exception {
        String email = "cliente.%d@teste.com".formatted(System.nanoTime());
        JsonNode cliente = json(mockMvc.perform(post("/clients")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"nome\":\"%s\",\"email\":\"%s\","
                                + "\"tipoPessoa\":\"FISICA\"}").formatted(nome, email)))
                .andExpect(status().isCreated())
                .andReturn());
        return cliente.get("id").asLong();
    }

    /** Pedido com 2 itens (2 × 40,00 e 1 × 20,00) e desconto de 10,00 → total 90,00. */
    private JsonNode criarPedido(long clienteId) throws Exception {
        return json(mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"clienteId\":%d,\"desconto\":10.00,\"itens\":["
                                + "{\"nomePeca\":\"Suporte de parede\",\"quantidade\":2,"
                                + "\"precoUnitario\":40.00},"
                                + "{\"nomePeca\":\"Chaveiro\",\"precoUnitario\":20.00}]}")
                                .formatted(clienteId)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void alterarStatus(long pedidoId, String status) throws Exception {
        mockMvc.perform(patch("/orders/{id}/status", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"%s\"}".formatted(status)))
                .andExpect(status().isOk());
    }
}
