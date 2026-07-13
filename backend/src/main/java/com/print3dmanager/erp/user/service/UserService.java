package com.print3dmanager.erp.user.service;

import com.print3dmanager.erp.common.dto.PageResponse;
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
import com.print3dmanager.erp.user.repository.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio de usuários: CRUD com soft delete, unicidade de
 * e-mail e troca de senha com revogação das sessões ativas.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listar(String busca, Role role, Boolean ativo,
                                             Pageable pageable) {
        return PageResponse.de(
                userRepository.findAll(UserSpecifications.comFiltros(busca, role, ativo), pageable)
                        .map(userMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse buscarPorId(Long id) {
        return userMapper.toResponse(obterUsuario(id));
    }

    @Transactional
    public UserResponse criar(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResourceConflictException("Já existe um usuário com este e-mail.");
        }

        User usuario = userMapper.toEntity(request);
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        return userMapper.toResponse(userRepository.save(usuario));
    }

    @Transactional
    public UserResponse atualizar(Long id, UserUpdateRequest request) {
        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ResourceConflictException("Já existe outro usuário com este e-mail.");
        }

        User usuario = obterUsuario(id);
        userMapper.atualizar(usuario, request);
        return userMapper.toResponse(usuario);
    }

    /** Soft delete: desativa o usuário e revoga suas sessões (acesso cai na hora). */
    @Transactional
    public void desativar(Long id, Long idUsuarioLogado) {
        if (id.equals(idUsuarioLogado)) {
            throw new BusinessException("Você não pode desativar o próprio usuário.");
        }

        User usuario = obterUsuario(id);
        usuario.setAtivo(false);
        refreshTokenRepository.revogarTodosDoUsuario(id);
    }

    @Transactional
    public UserResponse reativar(Long id) {
        User usuario = obterUsuario(id);
        usuario.setAtivo(true);
        return userMapper.toResponse(usuario);
    }

    @Transactional(readOnly = true)
    public UserResponse buscarPorEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", email));
    }

    /** Troca de senha do próprio usuário; revoga os refresh tokens ativos. */
    @Transactional
    public void alterarSenha(Long idUsuarioLogado, ChangePasswordRequest request) {
        User usuario = obterUsuario(idUsuarioLogado);

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new BusinessException("Senha atual incorreta.");
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        refreshTokenRepository.revogarTodosDoUsuario(usuario.getId());
    }

    private User obterUsuario(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
    }
}
