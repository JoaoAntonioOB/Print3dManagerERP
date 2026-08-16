package com.print3dmanager.erp.report;

import com.print3dmanager.erp.testsupport.AbstractApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fronteiras de autorização dos relatórios em PDF via HTTP contra o banco
 * real: pedidos e consumo de filamento abertos a todos os perfis internos;
 * o financeiro restrito a ADMINISTRADOR e FINANCEIRO — OPERADOR e
 * VISUALIZADOR recebem 403 em /reports/financeiro.
 */
class ReportAuthorizationIntegrationTest extends AbstractApiIntegrationTest {

    private String admin;

    @BeforeEach
    void autenticar() throws Exception {
        admin = loginAdmin();
    }

    @Test
    @DisplayName("/reports/pedidos e /reports/consumo-filamento: qualquer perfil interno acessa")
    void relatoriosAbertosATodosOsPerfis() throws Exception {
        String operador = criarUsuarioEAutenticar("OPERADOR");
        String visualizador = criarUsuarioEAutenticar("VISUALIZADOR");
        String financeiro = criarUsuarioEAutenticar("FINANCEIRO");

        for (String token : new String[]{admin, operador, visualizador, financeiro}) {
            mockMvc.perform(get("/reports/pedidos")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));

            mockMvc.perform(get("/reports/consumo-filamento")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        }
    }

    @Test
    @DisplayName("/reports/financeiro: ADMINISTRADOR e FINANCEIRO acessam (200)")
    void relatorioFinanceiroPermiteAdminEFinanceiro() throws Exception {
        String financeiro = criarUsuarioEAutenticar("FINANCEIRO");

        mockMvc.perform(get("/reports/financeiro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        mockMvc.perform(get("/reports/financeiro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(financeiro)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("/reports/financeiro: OPERADOR e VISUALIZADOR recebem 403")
    void relatorioFinanceiroBloqueiaOperadorEVisualizador() throws Exception {
        String operador = criarUsuarioEAutenticar("OPERADOR");
        String visualizador = criarUsuarioEAutenticar("VISUALIZADOR");

        mockMvc.perform(get("/reports/financeiro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(operador)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/reports/financeiro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(visualizador)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("relatórios sem token respondem 401")
    void relatoriosSemTokenRespondem401() throws Exception {
        mockMvc.perform(get("/reports/pedidos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/reports/financeiro")).andExpect(status().isUnauthorized());
    }

    // ===== helpers =====

    private String criarUsuarioEAutenticar(String role) throws Exception {
        String email = "%s.report.%d@print3d.com".formatted(role.toLowerCase(), System.nanoTime());
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
