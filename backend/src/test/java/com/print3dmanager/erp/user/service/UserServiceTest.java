package com.print3dmanager.erp.user.service;

import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceConflictException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.security.auth.RefreshTokenRepository;
import com.print3dmanager.erp.user.dto.ChangePasswordRequest;
import com.print3dmanager.erp.user.dto.UserCreateRequest;
import com.print3dmanager.erp.user.dto.UserResponse;
import com.print3dmanager.erp.user.dto.UserUpdateRequest;
import com.print3dmanager.erp.user.mapper.UserMapper;
import com.print3dmanager.erp.user.model.Role;
import com.print3dmanager.erp.user.model.User;
import com.print3dmanager.erp.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras de negócio de {@link UserService}: unicidade de e-mail (cadastro e
 * edição), soft delete com revogação de sessões e a proteção contra
 * autodesativação, e troca de senha (senha atual incorreta rejeitada,
 * sucesso revoga todas as sessões).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService service;

    @Test
    @DisplayName("criar: e-mail já cadastrado gera conflito (409), nada é salvo")
    void criarRejeitaEmailDuplicado() {
        UserCreateRequest request = new UserCreateRequest("Novo", "existe@print3d.com",
                "senha123", Role.OPERADOR);
        when(userRepository.existsByEmailIgnoreCase("existe@print3d.com")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(ResourceConflictException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("criar: senha é codificada antes de salvar (nunca texto plano)")
    void criarCodificaSenha() {
        UserCreateRequest request = new UserCreateRequest("Novo", "novo@print3d.com",
                "senha123", Role.OPERADOR);
        User entidade = new User();
        when(userRepository.existsByEmailIgnoreCase("novo@print3d.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(entidade);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-bcrypt");
        when(userRepository.save(entidade)).thenReturn(entidade);
        when(userMapper.toResponse(entidade)).thenReturn(resposta());

        service.criar(request);

        assertThat(entidade.getSenha()).isEqualTo("hash-bcrypt");
    }

    @Test
    @DisplayName("atualizar: e-mail já usado por outro usuário gera conflito (409)")
    void atualizarRejeitaEmailDuplicado() {
        UserUpdateRequest request = new UserUpdateRequest("Nome", "outro@print3d.com",
                Role.OPERADOR);
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("outro@print3d.com", 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, request))
                .isInstanceOf(ResourceConflictException.class);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("atualizar: id inexistente → 404")
    void atualizarComIdInexistente() {
        UserUpdateRequest request = new UserUpdateRequest("Nome", "livre@print3d.com",
                Role.OPERADOR);
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("livre@print3d.com", 1L))
                .thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("desativar: usuário não pode desativar a si mesmo")
    void desativarRejeitaAutodesativacao() {
        assertThatThrownBy(() -> service.desativar(5L, 5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("próprio usuário");
        verify(userRepository, never()).findById(any());
        verify(refreshTokenRepository, never()).revogarTodosDoUsuario(any());
    }

    @Test
    @DisplayName("desativar: soft delete marca ativo = false e revoga todas as sessões")
    void desativarMarcaInativoERevogaSessoes() {
        User usuario = usuario(1L, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));

        service.desativar(1L, 9L);

        assertThat(usuario.isAtivo()).isFalse();
        verify(refreshTokenRepository).revogarTodosDoUsuario(1L);
    }

    @Test
    @DisplayName("reativar: volta ativo = true")
    void reativarMarcaAtivo() {
        User usuario = usuario(1L, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(userMapper.toResponse(usuario)).thenReturn(resposta());

        service.reativar(1L);

        assertThat(usuario.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("alterarSenha: senha atual incorreta é rejeitada, sessões preservadas")
    void alterarSenhaRejeitaSenhaAtualIncorreta() {
        User usuario = usuario(1L, true);
        usuario.setSenha("hash-antigo");
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "hash-antigo")).thenReturn(false);

        assertThatThrownBy(() -> service.alterarSenha(1L,
                new ChangePasswordRequest("errada", "novaSenha123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("incorreta");
        verify(refreshTokenRepository, never()).revogarTodosDoUsuario(any());
    }

    @Test
    @DisplayName("alterarSenha: sucesso codifica a nova senha e revoga todas as sessões")
    void alterarSenhaComSucessoRevogaSessoes() {
        User usuario = usuario(1L, true);
        usuario.setSenha("hash-antigo");
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("atual123", "hash-antigo")).thenReturn(true);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hash-novo");

        service.alterarSenha(1L, new ChangePasswordRequest("atual123", "novaSenha123"));

        assertThat(usuario.getSenha()).isEqualTo("hash-novo");
        verify(refreshTokenRepository).revogarTodosDoUsuario(1L);
    }

    @Test
    @DisplayName("buscarPorEmail: e-mail inexistente → 404")
    void buscarPorEmailInexistente() {
        when(userRepository.findByEmailIgnoreCase("fantasma@print3d.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorEmail("fantasma@print3d.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== helpers =====

    private User usuario(Long id, boolean ativo) {
        User usuario = new User();
        usuario.setId(id);
        usuario.setNome("Usuário Teste");
        usuario.setEmail("usuario@print3d.com");
        usuario.setRole(Role.OPERADOR);
        usuario.setAtivo(ativo);
        return usuario;
    }

    private UserResponse resposta() {
        return new UserResponse(1L, "Usuário Teste", "usuario@print3d.com", Role.OPERADOR, true,
                Instant.now(), Instant.now());
    }
}
