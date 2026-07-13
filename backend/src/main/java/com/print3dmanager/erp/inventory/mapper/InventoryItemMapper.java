package com.print3dmanager.erp.inventory.mapper;

import com.print3dmanager.erp.inventory.dto.InventoryItemCreateRequest;
import com.print3dmanager.erp.inventory.dto.InventoryItemResponse;
import com.print3dmanager.erp.inventory.dto.InventoryItemUpdateRequest;
import com.print3dmanager.erp.inventory.model.InventoryItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Conversões entidade ↔ DTO de itens de estoque. No cadastro, campos omitidos
 * assumem os mesmos defaults do banco (quantidades 0, unidade UN);
 * na atualização a quantidade não é tocada (só via movimentação).
 */
@Mapper
public interface InventoryItemMapper {

    InventoryItemResponse toResponse(InventoryItem item);

    @Mapping(target = "quantidade", source = "quantidade", defaultValue = "0")
    @Mapping(target = "quantidadeMinima", source = "quantidadeMinima", defaultValue = "0")
    @Mapping(target = "unidadeMedida", source = "unidadeMedida", defaultValue = "UN")
    InventoryItem toEntity(InventoryItemCreateRequest request);

    void atualizar(@MappingTarget InventoryItem item, InventoryItemUpdateRequest request);
}
