package com.print3dmanager.erp.quote;

import com.fasterxml.jackson.databind.JsonNode;
import com.print3dmanager.erp.testsupport.AbstractApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo do orçamento via HTTP contra o banco real: precificação com markup
 * herdado da configuração global, envio, aprovação pelo link público
 * (sem autenticação e sem custos internos) e conversão em pedido.
 */
class QuoteFlowIntegrationTest extends AbstractApiIntegrationTest {

    private String admin;

    @BeforeEach
    void autenticar() throws Exception {
        admin = loginAdmin();
        // garante a configuração global de custos usada pela precificação
        mockMvc.perform(put("/printers/config")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorKwh\":0.95,\"valorHoraMaquina\":8.00,"
                                + "\"custoDesgasteHora\":1.50,\"markupPadrao\":120.00}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("fluxo completo: orçar com markup herdado → enviar → aprovar no link público → converter")
    void fluxoCompletoDoOrcamento() throws Exception {
        long clienteId = criarCliente("Cliente Orçamento");
        JsonNode orcamento = criarOrcamento(clienteId);
        long orcamentoId = orcamento.get("id").asLong();
        String shareToken = orcamento.get("shareToken").asText();

        // markup omitido no POST herda o markupPadrao global (120%)
        assertThat(orcamento.get("numero").asText()).matches("ORC-\\d{4}-\\d{4}");
        assertThat(orcamento.get("markup").decimalValue()).isEqualByComparingTo("120.00");
        // sem filamento/impressora: 2 h × (8,00 + 1,50) = 19,00 × 2,2 = 41,80
        assertThat(orcamento.get("custoTotal").decimalValue()).isEqualByComparingTo("19.00");
        assertThat(orcamento.get("precoSugerido").decimalValue()).isEqualByComparingTo("41.80");

        // RASCUNHO não é visível no link público
        mockMvc.perform(get("/public/quotes/{token}", shareToken))
                .andExpect(status().isNotFound());

        alterarStatus(orcamentoId, "ENVIADO");

        // visão pública sem login: preço efetivo presente, custos internos ausentes
        mockMvc.perform(get("/public/quotes/{token}", shareToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENVIADO"))
                .andExpect(jsonPath("$.preco").value(41.80))
                .andExpect(jsonPath("$.custoFilamento").doesNotExist())
                .andExpect(jsonPath("$.markup").doesNotExist())
                .andExpect(jsonPath("$.precoSugerido").doesNotExist());

        // cliente aprova pelo link público
        mockMvc.perform(post("/public/quotes/{token}/aprovar", shareToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"));

        // conversão gera o pedido com item único no preço efetivo
        JsonNode pedido = json(mockMvc.perform(post("/quotes/{id}/converter", orcamentoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isCreated())
                .andReturn());
        assertThat(pedido.get("numero").asText()).matches("PED-\\d{4}-\\d{4}");
        assertThat(pedido.get("valorTotal").decimalValue()).isEqualByComparingTo("41.80");
        assertThat(pedido.get("itens")).hasSize(1);

        mockMvc.perform(get("/quotes/{id}", orcamentoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONVERTIDO"))
                .andExpect(jsonPath("$.pedidoId").value(pedido.get("id").asLong()));
    }

    @Test
    @DisplayName("cliente recusa pelo link público e a conversão é bloqueada")
    void recusaPublicaBloqueiaConversao() throws Exception {
        long clienteId = criarCliente("Cliente Recusa");
        JsonNode orcamento = criarOrcamento(clienteId);
        long orcamentoId = orcamento.get("id").asLong();
        String shareToken = orcamento.get("shareToken").asText();

        alterarStatus(orcamentoId, "ENVIADO");
        mockMvc.perform(post("/public/quotes/{token}/recusar", shareToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADO"));

        mockMvc.perform(post("/quotes/{id}/converter", orcamentoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("token público inválido ou desconhecido responde 404")
    void tokenPublicoInvalido() throws Exception {
        mockMvc.perform(get("/public/quotes/{token}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/public/quotes/{token}", "nao-e-um-uuid"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("corrida: duas conversões concorrentes do mesmo orçamento aprovado "
            + "geram só um pedido — a perdedora recebe erro de negócio, não um 500 "
            + "nem um segundo pedido")
    void conversaoConcorrenteGeraApenasUmPedido() throws Exception {
        long clienteId = criarCliente("Cliente Corrida Conversao");
        JsonNode orcamento = criarOrcamento(clienteId);
        long orcamentoId = orcamento.get("id").asLong();
        String shareToken = orcamento.get("shareToken").asText();

        alterarStatus(orcamentoId, "ENVIADO");
        mockMvc.perform(post("/public/quotes/{token}/aprovar", shareToken))
                .andExpect(status().isOk());

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CyclicBarrier barreira = new CyclicBarrier(threads);
        try {
            List<Callable<Integer>> chamadas = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                chamadas.add(() -> {
                    barreira.await();
                    return mockMvc.perform(post("/quotes/{id}/converter", orcamentoId)
                                    .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                            .andReturn().getResponse().getStatus();
                });
            }
            List<Future<Integer>> resultados = pool.invokeAll(chamadas);
            List<Integer> statusCodes = new ArrayList<>();
            for (Future<Integer> resultado : resultados) {
                statusCodes.add(resultado.get());
            }

            // exatamente uma conversão vence (201) e a outra é rejeitada com
            // erro de negócio (400 — orçamento não está mais APROVADO)
            assertThat(statusCodes).containsExactlyInAnyOrder(201, 400);
        } finally {
            pool.shutdownNow();
        }

        // só um pedido foi criado para este cliente, apesar das duas chamadas
        mockMvc.perform(get("/orders").queryParam("clienteId", String.valueOf(clienteId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/quotes/{id}", orcamentoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONVERTIDO"));
    }

    // ===== helpers =====

    private long criarCliente(String nome) throws Exception {
        String email = "cliente.orc.%d@teste.com".formatted(System.nanoTime());
        JsonNode cliente = json(mockMvc.perform(post("/clients")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"nome\":\"%s\",\"email\":\"%s\","
                                + "\"tipoPessoa\":\"FISICA\"}").formatted(nome, email)))
                .andExpect(status().isCreated())
                .andReturn());
        return cliente.get("id").asLong();
    }

    /** Orçamento de 2 h sem impressora/filamento e sem markup (herda o global). */
    private JsonNode criarOrcamento(long clienteId) throws Exception {
        return json(mockMvc.perform(post("/quotes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"clienteId\":%d,\"descricao\":\"Suporte de parede\","
                                + "\"tempoImpressaoMinutos\":120,\"pesoEstimadoG\":200}")
                                .formatted(clienteId)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void alterarStatus(long orcamentoId, String status) throws Exception {
        mockMvc.perform(patch("/quotes/{id}/status", orcamentoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"%s\"}".formatted(status)))
                .andExpect(status().isOk());
    }
}
