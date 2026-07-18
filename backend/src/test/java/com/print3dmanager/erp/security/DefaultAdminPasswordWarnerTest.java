package com.print3dmanager.erp.security;

import com.print3dmanager.erp.user.model.Role;
import com.print3dmanager.erp.user.model.User;
import com.print3dmanager.erp.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAdminPasswordWarnerTest {

    @Mock
    private UserRepository userRepository;
    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private DefaultAdminPasswordWarner warner;

    @Test
    @DisplayName("admin ativo com a senha default da V11 dispara o aviso")
    void detectaSenhaDefault() {
        when(userRepository.findByEmail(DefaultAdminPasswordWarner.EMAIL_ADMIN))
                .thenReturn(Optional.of(admin("admin123", true)));

        assertThat(warner.senhaDefaultEmUso()).isTrue();
    }

    @Test
    @DisplayName("senha trocada não dispara o aviso")
    void senhaTrocadaNaoAvisa() {
        when(userRepository.findByEmail(DefaultAdminPasswordWarner.EMAIL_ADMIN))
                .thenReturn(Optional.of(admin("senha-forte-!2026", true)));

        assertThat(warner.senhaDefaultEmUso()).isFalse();
    }

    @Test
    @DisplayName("admin desativado ou inexistente não dispara o aviso")
    void adminAusenteOuInativoNaoAvisa() {
        when(userRepository.findByEmail(DefaultAdminPasswordWarner.EMAIL_ADMIN))
                .thenReturn(Optional.of(admin("admin123", false)));
        assertThat(warner.senhaDefaultEmUso()).isFalse();

        when(userRepository.findByEmail(DefaultAdminPasswordWarner.EMAIL_ADMIN))
                .thenReturn(Optional.empty());
        assertThat(warner.senhaDefaultEmUso()).isFalse();
    }

    private User admin(String senhaEmTextoPlano, boolean ativo) {
        User admin = new User();
        admin.setId(1L);
        admin.setNome("Administrador");
        admin.setEmail(DefaultAdminPasswordWarner.EMAIL_ADMIN);
        admin.setSenha(new BCryptPasswordEncoder().encode(senhaEmTextoPlano));
        admin.setRole(Role.ADMINISTRADOR);
        admin.setAtivo(ativo);
        return admin;
    }
}
