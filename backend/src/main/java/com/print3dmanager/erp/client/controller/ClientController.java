package com.print3dmanager.erp.client.controller;

import com.print3dmanager.erp.client.dto.ClientCreateRequest;
import com.print3dmanager.erp.client.dto.ClientResponse;
import com.print3dmanager.erp.client.dto.ClientUpdateRequest;
import com.print3dmanager.erp.client.model.PersonType;
import com.print3dmanager.erp.client.service.ClientService;
import com.print3dmanager.erp.common.dto.PageResponse;
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
 * Gestão de clientes: ADMINISTRADOR e OPERADOR gerenciam;
 * FINANCEIRO e VISUALIZADOR apenas consultam.
 */
@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestão de clientes da empresa")
public class ClientController {

    private static final String PODE_GERENCIAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR')";
    private static final String PODE_CONSULTAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR')";

    private final ClientService clientService;

    @GetMapping
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Lista clientes com paginação e filtros opcionais")
    public PageResponse<ClientResponse> listar(
            @Parameter(description = "Busca por nome, e-mail ou CPF/CNPJ")
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) PersonType tipoPessoa,
            @RequestParam(required = false) Boolean ativo,
            @ParameterObject @PageableDefault(sort = "nome") Pageable pageable) {
        return clientService.listar(busca, tipoPessoa, ativo, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Busca um cliente pelo id")
    public ClientResponse buscarPorId(@PathVariable Long id) {
        return clientService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo cliente")
    public ClientResponse criar(@Valid @RequestBody ClientCreateRequest request) {
        return clientService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Atualiza os dados de um cliente")
    public ClientResponse atualizar(@PathVariable Long id,
                                    @Valid @RequestBody ClientUpdateRequest request) {
        return clientService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa um cliente (soft delete)",
            description = "O cliente sai das listagens ativas mas o histórico "
                    + "de pedidos e orçamentos é preservado.")
    public void desativar(@PathVariable Long id) {
        clientService.desativar(id);
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Reativa um cliente desativado")
    public ClientResponse reativar(@PathVariable Long id) {
        return clientService.reativar(id);
    }
}
