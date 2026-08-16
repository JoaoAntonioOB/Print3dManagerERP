package com.print3dmanager.erp;

import com.print3dmanager.erp.testsupport.AbstractIntegrationTest;
import com.print3dmanager.erp.user.model.Role;
import com.print3dmanager.erp.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de carregamento de contexto contra PostgreSQL real (Testcontainers):
 * o boot com {@code ddl-auto: validate} garante que as entidades JPA casam
 * com o schema produzido pelas migrações Flyway.
 */
class Print3dManagerErpApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("contexto sobe com o schema do Flyway validado pelo Hibernate")
    void contextoCarrega() {
        assertThat(context).isNotNull();
        assertThat(POSTGRES.isRunning()).isTrue();
    }

    @Test
    @DisplayName("todas as migrações Flyway foram aplicadas com sucesso")
    void migracoesAplicadas() {
        Integer falhas = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = false",
                Integer.class);
        Integer aplicadas = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version is not null",
                Integer.class);

        assertThat(falhas).isZero();
        assertThat(aplicadas).isGreaterThanOrEqualTo(11);
    }

    @Test
    @DisplayName("usuário admin inicial da migração V11 está semeado")
    void adminInicialSemeado() {
        assertThat(userRepository.findByEmailIgnoreCase("admin@print3d.com"))
                .hasValueSatisfying(admin -> {
                    assertThat(admin.getRole()).isEqualTo(Role.ADMINISTRADOR);
                    assertThat(admin.isAtivo()).isTrue();
                });
    }
}
