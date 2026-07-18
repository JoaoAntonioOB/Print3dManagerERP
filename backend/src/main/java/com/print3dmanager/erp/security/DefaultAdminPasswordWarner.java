package com.print3dmanager.erp.security;

import com.print3dmanager.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Alerta de segurança no boot: o usuário administrador semeado pela
 * migração V11 não deve continuar com a senha default fora do ambiente
 * de desenvolvimento. Só avisa — não bloqueia o boot, para não derrubar
 * o ambiente de demonstração/avaliação.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultAdminPasswordWarner implements ApplicationRunner {

    static final String EMAIL_ADMIN = "admin@print3d.com";
    static final String SENHA_DEFAULT = "admin123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (senhaDefaultEmUso()) {
            log.warn("AVISO DE SEGURANÇA: o usuário {} ainda usa a senha default da "
                            + "instalação. Troque-a em PATCH /users/me/senha (ou pela "
                            + "tela de troca de senha) antes de expor o sistema.",
                    EMAIL_ADMIN);
        }
    }

    /** O admin semeado existe, está ativo e ainda autentica com a senha default? */
    boolean senhaDefaultEmUso() {
        return userRepository.findByEmail(EMAIL_ADMIN)
                .filter(admin -> admin.isAtivo()
                        && passwordEncoder.matches(SENHA_DEFAULT, admin.getSenha()))
                .isPresent();
    }
}
