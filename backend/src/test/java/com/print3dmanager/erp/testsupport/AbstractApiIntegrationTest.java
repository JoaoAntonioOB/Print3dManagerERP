package com.print3dmanager.erp.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de API: MockMvc sobre o contexto completo (segurança real,
 * banco real via Testcontainers). O MockMvc não usa o context path /api —
 * as rotas aqui são as dos controllers (ex.: /auth/login, /orders).
 */
@AutoConfigureMockMvc
public abstract class AbstractApiIntegrationTest extends AbstractIntegrationTest {

    /** Credenciais do admin semeado pela migração V11. */
    protected static final String ADMIN_EMAIL = "admin@print3d.com";
    protected static final String ADMIN_SENHA = "admin123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** Faz login e devolve o corpo completo da resposta (tokens + usuário). */
    protected JsonNode loginCompleto(String email, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("email", email)
                                .put("senha", senha)
                                .toString()))
                .andExpect(status().isOk())
                .andReturn();
        return json(result);
    }

    /** Access token do admin inicial. */
    protected String loginAdmin() throws Exception {
        return loginCompleto(ADMIN_EMAIL, ADMIN_SENHA).get("accessToken").asText();
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    protected String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
