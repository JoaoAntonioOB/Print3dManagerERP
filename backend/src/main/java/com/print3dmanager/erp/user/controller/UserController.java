package com.print3dmanager.erp.user.controller;

import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.security.SecurityUser;
import com.print3dmanager.erp.user.dto.ChangePasswordRequest;
import com.print3dmanager.erp.user.dto.UserCreateRequest;
import com.print3dmanager.erp.user.dto.UserResponse;
import com.print3dmanager.erp.user.dto.UserUpdateRequest;
import com.print3dmanager.erp.user.model.Role;
import com.print3dmanager.erp.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestão de usuários (restrita a ADMINISTRADOR) e endpoints /me
 * do próprio usuário autenticado (qualquer role).
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Gestão de usuários do sistema")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Lista usuários com paginação e filtros opcionais")
    public PageResponse<UserResponse> listar(
            @Parameter(description = "Busca por nome ou e-mail") @RequestParam(required = false) String busca,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean ativo,
            @ParameterObject @PageableDefault(sort = "nome") Pageable pageable) {
        return userService.listar(busca, role, ativo, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Busca um usuário pelo id")
    public UserResponse buscarPorId(@PathVariable Long id) {
        return userService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um novo usuário")
    public UserResponse criar(@Valid @RequestBody UserCreateRequest request) {
        return userService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Atualiza nome, e-mail e papel de um usuário")
    public UserResponse atualizar(@PathVariable Long id,
                                  @Valid @RequestBody UserUpdateRequest request) {
        return userService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa um usuário (soft delete)",
            description = "O usuário é desativado e suas sessões revogadas. "
                    + "Não é possível desativar o próprio usuário.")
    public void desativar(@PathVariable Long id,
                          @AuthenticationPrincipal SecurityUser principal) {
        userService.desativar(id, principal.getUser().getId());
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Reativa um usuário desativado")
    public UserResponse reativar(@PathVariable Long id) {
        return userService.reativar(id);
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado")
    public UserResponse me(@AuthenticationPrincipal SecurityUser principal) {
        return userService.buscarPorId(principal.getUser().getId());
    }

    @PatchMapping("/me/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Troca a senha do usuário autenticado",
            description = "Exige a senha atual. Todas as sessões (refresh tokens) são "
                    + "revogadas — é necessário fazer login novamente.")
    public void alterarSenha(@AuthenticationPrincipal SecurityUser principal,
                             @Valid @RequestBody ChangePasswordRequest request) {
        userService.alterarSenha(principal.getUser().getId(), request);
    }
}
