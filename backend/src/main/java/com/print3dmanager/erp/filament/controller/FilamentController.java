package com.print3dmanager.erp.filament.controller;

import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.filament.dto.FilamentCreateRequest;
import com.print3dmanager.erp.filament.dto.FilamentResponse;
import com.print3dmanager.erp.filament.dto.FilamentStockRequest;
import com.print3dmanager.erp.filament.dto.FilamentUpdateRequest;
import com.print3dmanager.erp.filament.model.FilamentMaterial;
import com.print3dmanager.erp.filament.service.FilamentService;
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
 * Gestão de filamentos/resinas: ADMINISTRADOR e OPERADOR gerenciam;
 * FINANCEIRO e VISUALIZADOR apenas consultam.
 */
@RestController
@RequestMapping("/filaments")
@RequiredArgsConstructor
@Tag(name = "Filamentos", description = "Gestão de filamentos/resinas e estoque em gramas")
public class FilamentController {

    private static final String PODE_GERENCIAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR')";
    private static final String PODE_CONSULTAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR')";

    private final FilamentService filamentService;

    @GetMapping
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Lista filamentos com paginação e filtros opcionais")
    public PageResponse<FilamentResponse> listar(
            @Parameter(description = "Busca por nome, marca ou cor")
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) FilamentMaterial material,
            @RequestParam(required = false) Boolean ativo,
            @Parameter(description = "true = apenas com estoque baixo (quantidade ≤ mínimo)")
            @RequestParam(required = false) Boolean estoqueBaixo,
            @ParameterObject @PageableDefault(sort = "nome") Pageable pageable) {
        return filamentService.listar(busca, material, ativo, estoqueBaixo, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Busca um filamento pelo id")
    public FilamentResponse buscarPorId(@PathVariable Long id) {
        return filamentService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo filamento")
    public FilamentResponse criar(@Valid @RequestBody FilamentCreateRequest request) {
        return filamentService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Atualiza os dados de um filamento",
            description = "O estoque não é alterado aqui — use o endpoint de movimentação.")
    public FilamentResponse atualizar(@PathVariable Long id,
                                      @Valid @RequestBody FilamentUpdateRequest request) {
        return filamentService.atualizar(id, request);
    }

    @PatchMapping("/{id}/estoque")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Movimenta o estoque em gramas (ENTRADA ou SAIDA)",
            description = "Saída maior que o saldo disponível é recusada (400), assim como "
                    + "movimentações em filamentos desativados.")
    public FilamentResponse movimentarEstoque(@PathVariable Long id,
                                              @Valid @RequestBody FilamentStockRequest request) {
        return filamentService.movimentarEstoque(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa um filamento (soft delete)")
    public void desativar(@PathVariable Long id) {
        filamentService.desativar(id);
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Reativa um filamento desativado")
    public FilamentResponse reativar(@PathVariable Long id) {
        return filamentService.reativar(id);
    }
}
