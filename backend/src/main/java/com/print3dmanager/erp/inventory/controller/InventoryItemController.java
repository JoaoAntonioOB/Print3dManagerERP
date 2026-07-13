package com.print3dmanager.erp.inventory.controller;

import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.inventory.dto.InventoryItemCreateRequest;
import com.print3dmanager.erp.inventory.dto.InventoryItemResponse;
import com.print3dmanager.erp.inventory.dto.InventoryItemUpdateRequest;
import com.print3dmanager.erp.inventory.dto.InventoryStockRequest;
import com.print3dmanager.erp.inventory.service.InventoryItemService;
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
 * Gestão de insumos gerais do estoque (exceto filamentos): ADMINISTRADOR e
 * OPERADOR gerenciam; FINANCEIRO e VISUALIZADOR apenas consultam.
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "Estoque", description = "Gestão de insumos gerais (peças, embalagens, componentes)")
public class InventoryItemController {

    private static final String PODE_GERENCIAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR')";
    private static final String PODE_CONSULTAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR')";

    private final InventoryItemService inventoryItemService;

    @GetMapping
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Lista itens de estoque com paginação e filtros opcionais")
    public PageResponse<InventoryItemResponse> listar(
            @Parameter(description = "Busca por nome, descrição, categoria ou localização")
            @RequestParam(required = false) String busca,
            @Parameter(description = "Categoria exata (sem diferenciar maiúsculas)")
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean ativo,
            @Parameter(description = "true = apenas com estoque baixo (quantidade ≤ mínima)")
            @RequestParam(required = false) Boolean estoqueBaixo,
            @ParameterObject @PageableDefault(sort = "nome") Pageable pageable) {
        return inventoryItemService.listar(busca, categoria, ativo, estoqueBaixo, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Busca um item de estoque pelo id")
    public InventoryItemResponse buscarPorId(@PathVariable Long id) {
        return inventoryItemService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo item de estoque")
    public InventoryItemResponse criar(@Valid @RequestBody InventoryItemCreateRequest request) {
        return inventoryItemService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Atualiza os dados de um item de estoque",
            description = "A quantidade não é alterada aqui — use o endpoint de movimentação.")
    public InventoryItemResponse atualizar(@PathVariable Long id,
                                           @Valid @RequestBody InventoryItemUpdateRequest request) {
        return inventoryItemService.atualizar(id, request);
    }

    @PatchMapping("/{id}/estoque")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Movimenta o estoque na unidade do item (ENTRADA ou SAIDA)",
            description = "Saída maior que o saldo disponível é recusada (400), assim como "
                    + "movimentações em itens desativados.")
    public InventoryItemResponse movimentarEstoque(@PathVariable Long id,
                                                   @Valid @RequestBody InventoryStockRequest request) {
        return inventoryItemService.movimentarEstoque(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa um item de estoque (soft delete)")
    public void desativar(@PathVariable Long id) {
        inventoryItemService.desativar(id);
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Reativa um item de estoque desativado")
    public InventoryItemResponse reativar(@PathVariable Long id) {
        return inventoryItemService.reativar(id);
    }
}
