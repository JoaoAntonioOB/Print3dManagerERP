package com.print3dmanager.erp;

import org.junit.jupiter.api.Test;

class Print3dManagerErpApplicationTests {

    /**
     * Teste de sanidade do módulo. O teste real de carregamento de contexto
     * (com Testcontainers + PostgreSQL) será implementado na etapa de Testes,
     * pois depende do banco de dados e das migrações Flyway.
     */
    @Test
    void applicationClassExists() {
        org.assertj.core.api.Assertions.assertThat(Print3dManagerErpApplication.class).isNotNull();
    }
}
