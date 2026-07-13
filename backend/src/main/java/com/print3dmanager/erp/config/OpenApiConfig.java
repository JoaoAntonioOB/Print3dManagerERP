package com.print3dmanager.erp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação OpenAPI com esquema de autenticação Bearer JWT —
 * habilita o botão "Authorize" no Swagger UI para testar endpoints
 * protegidos com o token obtido em /auth/login.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI print3dManagerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Print3D Manager ERP API")
                        .description("""
                                API do sistema ERP para gerenciamento de empresas de impressão 3D: \
                                clientes, pedidos, orçamentos, filamentos, estoque, impressoras, \
                                financeiro e relatórios.

                                Autentique-se em **POST /auth/login** e informe o access token \
                                no botão Authorize.""")
                        .version("1.0.0")
                        .contact(new Contact().name("Print3D Manager ERP")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
