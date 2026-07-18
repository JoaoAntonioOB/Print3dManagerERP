package com.print3dmanager.erp.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.print3dmanager.erp.testsupport.AbstractApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Upload/download/remoção do arquivo STL/3MF de itens de pedido via HTTP
 * contra o banco e o disco reais (upload-dir do perfil test).
 */
class OrderItemFileIntegrationTest extends AbstractApiIntegrationTest {

    private static final byte[] CONTEUDO_STL =
            "solid peca de teste".getBytes(StandardCharsets.UTF_8);

    private String admin;

    @BeforeEach
    void autenticar() throws Exception {
        admin = loginAdmin();
    }

    @Test
    @DisplayName("fluxo completo: anexar → baixar → substituir → remover")
    void fluxoCompletoDoArquivo() throws Exception {
        JsonNode pedido = criarPedido(criarCliente());
        long pedidoId = pedido.get("id").asLong();
        long itemId = pedido.get("itens").get(0).get("id").asLong();

        // anexar STL
        JsonNode itemComArquivo = json(mockMvc.perform(
                        multipart("/orders/{p}/itens/{i}/arquivo", pedidoId, itemId)
                                .file(arquivo("Suporte Parede.stl", CONTEUDO_STL))
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(itemComArquivo.get("arquivoModelo").asText()).startsWith("modelos/");

        // baixar: conteúdo idêntico, nome sanitizado no Content-Disposition
        MvcResult download = mockMvc.perform(
                        get("/orders/{p}/itens/{i}/arquivo", pedidoId, itemId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(status().isOk())
                .andReturn();
        assertThat(download.getResponse().getContentType()).isEqualTo("model/stl");
        assertThat(download.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .contains("Suporte_Parede.stl");
        assertThat(download.getResponse().getContentAsByteArray()).isEqualTo(CONTEUDO_STL);

        // substituir por um 3MF
        mockMvc.perform(multipart("/orders/{p}/itens/{i}/arquivo", pedidoId, itemId)
                        .file(arquivo("versao2.3mf", "conteudo 3mf".getBytes(StandardCharsets.UTF_8)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arquivoModelo").value(
                        org.hamcrest.Matchers.endsWith(".3mf")));

        // remover: 204 e o download passa a dar 404
        mockMvc.perform(delete("/orders/{p}/itens/{i}/arquivo", pedidoId, itemId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/orders/{p}/itens/{i}/arquivo", pedidoId, itemId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("extensão não suportada responde 400")
    void extensaoInvalida() throws Exception {
        JsonNode pedido = criarPedido(criarCliente());
        long pedidoId = pedido.get("id").asLong();
        long itemId = pedido.get("itens").get(0).get("id").asLong();

        mockMvc.perform(multipart("/orders/{p}/itens/{i}/arquivo", pedidoId, itemId)
                        .file(arquivo("peca.obj", CONTEUDO_STL))
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString(".stl e .3mf")));
    }

    @Test
    @DisplayName("pedido fora de PENDENTE não aceita upload, mas mantém o download")
    void uploadSomentePendente() throws Exception {
        JsonNode pedido = criarPedido(criarCliente());
        long pedidoId = pedido.get("id").asLong();
        long itemId = pedido.get("itens").get(0).get("id").asLong();

        mockMvc.perform(multipart("/orders/{p}/itens/{i}/arquivo", pedidoId, itemId)
                        .file(arquivo("peca.stl", CONTEUDO_STL))
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/orders/{id}/status", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EM_PRODUCAO\"}"))
                .andExpect(status().isOk());

        // upload e remoção bloqueados fora de PENDENTE
        mockMvc.perform(multipart("/orders/{p}/itens/{i}/arquivo", pedidoId, itemId)
                        .file(arquivo("nova.stl", CONTEUDO_STL))
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/orders/{p}/itens/{i}/arquivo", pedidoId, itemId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest());

        // download segue disponível durante a produção
        mockMvc.perform(get("/orders/{p}/itens/{i}/arquivo", pedidoId, itemId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("item de outro pedido responde 404")
    void itemDeOutroPedido() throws Exception {
        long clienteId = criarCliente();
        JsonNode pedidoA = criarPedido(clienteId);
        JsonNode pedidoB = criarPedido(clienteId);
        long itemDoB = pedidoB.get("itens").get(0).get("id").asLong();

        mockMvc.perform(multipart("/orders/{p}/itens/{i}/arquivo",
                        pedidoA.get("id").asLong(), itemDoB)
                        .file(arquivo("peca.stl", CONTEUDO_STL))
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNotFound());
    }

    // ===== helpers =====

    private MockMultipartFile arquivo(String nome, byte[] conteudo) {
        return new MockMultipartFile("arquivo", nome,
                MediaType.APPLICATION_OCTET_STREAM_VALUE, conteudo);
    }

    private long criarCliente() throws Exception {
        String email = "cliente.arq.%d@teste.com".formatted(System.nanoTime());
        JsonNode cliente = json(mockMvc.perform(post("/clients")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"nome\":\"Cliente Arquivos\",\"email\":\"%s\","
                                + "\"tipoPessoa\":\"FISICA\"}").formatted(email)))
                .andExpect(status().isCreated())
                .andReturn());
        return cliente.get("id").asLong();
    }

    private JsonNode criarPedido(long clienteId) throws Exception {
        return json(mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"clienteId\":%d,\"itens\":["
                                + "{\"nomePeca\":\"Peça com modelo\",\"precoUnitario\":30.00}]}")
                                .formatted(clienteId)))
                .andExpect(status().isCreated())
                .andReturn());
    }
}
