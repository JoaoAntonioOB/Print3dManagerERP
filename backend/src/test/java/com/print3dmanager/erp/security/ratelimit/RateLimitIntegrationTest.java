package com.print3dmanager.erp.security.ratelimit;

import com.print3dmanager.erp.testsupport.AbstractApiIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rate limit de ponta a ponta com limites baixos só neste contexto
 * (o perfil test usa limites folgados para não atrapalhar a suíte).
 * Cada teste usa um IP próprio — o contador é por IP.
 */
@TestPropertySource(properties = {
        "application.rate-limit.limite-auth=3",
        "application.rate-limit.limite-public=2",
})
class RateLimitIntegrationTest extends AbstractApiIntegrationTest {

    @Test
    @DisplayName("força bruta no login: a 4ª tentativa do mesmo IP responde 429 com Retry-After")
    void loginBloqueadoAposLimite() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(loginInvalido("10.9.9.1"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(loginInvalido("10.9.9.1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Muitas requisições")));
    }

    @Test
    @DisplayName("o bloqueio é por IP: outro endereço continua respondendo 401")
    void outroIpNaoEBloqueado() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(loginInvalido("10.9.9.2"));
        }

        mockMvc.perform(loginInvalido("10.9.9.3"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("link público de orçamento também é limitado")
    void rotaPublicaLimitada() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/public/quotes/{token}", UUID.randomUUID())
                            .with(ip("10.9.9.4")))
                    .andExpect(status().isNotFound());
        }

        mockMvc.perform(get("/public/quotes/{token}", UUID.randomUUID())
                        .with(ip("10.9.9.4")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("rotas autenticadas não passam pelo rate limit")
    void rotasAutenticadasNaoLimitadas() throws Exception {
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(get("/users").with(ip("10.9.9.5")))
                    .andExpect(status().isUnauthorized());
        }
    }

    private MockHttpServletRequestBuilder loginInvalido(String enderecoIp) {
        return post("/auth/login")
                .with(ip(enderecoIp))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"senha-errada\"}"
                        .formatted(ADMIN_EMAIL));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor ip(
            String enderecoIp) {
        return request -> {
            request.setRemoteAddr(enderecoIp);
            return request;
        };
    }
}
