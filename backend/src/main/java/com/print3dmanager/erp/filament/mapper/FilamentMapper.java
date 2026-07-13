package com.print3dmanager.erp.filament.mapper;

import com.print3dmanager.erp.filament.dto.FilamentCreateRequest;
import com.print3dmanager.erp.filament.dto.FilamentResponse;
import com.print3dmanager.erp.filament.dto.FilamentUpdateRequest;
import com.print3dmanager.erp.filament.model.Filament;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Conversões entidade ↔ DTO de filamentos. No cadastro, campos omitidos
 * assumem os mesmos defaults do banco (diâmetro 1.75, estoques 0);
 * na atualização o estoque não é tocado (só via movimentação).
 */
@Mapper
public interface FilamentMapper {

    FilamentResponse toResponse(Filament filament);

    @Mapping(target = "diametroMm", source = "diametroMm", defaultValue = "1.75")
    @Mapping(target = "quantidadeEstoqueG", source = "quantidadeEstoqueG", defaultValue = "0")
    @Mapping(target = "estoqueMinimoG", source = "estoqueMinimoG", defaultValue = "0")
    Filament toEntity(FilamentCreateRequest request);

    void atualizar(@MappingTarget Filament filament, FilamentUpdateRequest request);
}
