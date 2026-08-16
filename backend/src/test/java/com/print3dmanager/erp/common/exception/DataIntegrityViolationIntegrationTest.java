package com.print3dmanager.erp.common.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.print3dmanager.erp.testsupport.AbstractApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre o handler de {@link org.springframework.dao.DataIntegrityViolationException}:
 * excluir um pedido PENDENTE ainda referenciado por uma transação financeira
 * manual (FK sem ON DELETE) deve responder 409, não 500 cru.
 */
class DataIntegrityViolationIntegrationTest extends AbstractApiIntegrationTest {

    private String admin;

    @BeforeEach
    void autenticar() throws Exception {
        admin = loginAdmin();
    }

    @Test
    @DisplayName("excluir pedido referenciado por transação financeira responde 409, não 500")
    void exclusaoDePedidoReferenciadoRespondeConflito() throws Exception {
        long clienteId = criarCliente("Cliente Integridade de Dados");
        long pedidoId = criarPedido(clienteId);

        // transação financeira manual vinculada ao pedido, ainda PENDENTE
        mockMvc.perform(post("/financial/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"tipo\":\"RECEITA\",\"categoria\":\"Vendas\","
                                + "\"descricao\":\"Sinal do pedido\",\"valor\":50.00,"
                                + "\"dataTransacao\":\"2026-01-10\",\"pedidoId\":%d}")
                                .formatted(pedidoId)))
                .andExpect(status().isCreated());

        // FK sem ON DELETE em transacoes_financeiras.pedido_id → violação de integridade
        mockMvc.perform(delete("/orders/{id}", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("vinculado a outros dados")));
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

    private long criarPedido(long clienteId) throws Exception {
        JsonNode pedido = json(mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"clienteId\":%d,\"itens\":["
                                + "{\"nomePeca\":\"Chaveiro\",\"precoUnitario\":20.00}]}")
                                .formatted(clienteId)))
                .andExpect(status().isCreated())
                .andReturn());
        return pedido.get("id").asLong();
    }
}
