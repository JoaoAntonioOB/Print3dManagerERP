package com.print3dmanager.erp.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de integração: sobe UM PostgreSQL 16 real (Testcontainers)
 * compartilhado por toda a suíte — o padrão singleton container evita pagar o
 * custo de um banco por classe e mantém o contexto Spring cacheado entre as
 * classes de teste. O {@code @ServiceConnection} injeta url/usuário/senha do
 * container no DataSource, e o Flyway aplica as migrações reais no boot
 * (com {@code ddl-auto: validate}, o mesmo contrato de produção).
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
