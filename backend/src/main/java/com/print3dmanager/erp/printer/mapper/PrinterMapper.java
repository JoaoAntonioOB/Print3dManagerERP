package com.print3dmanager.erp.printer.mapper;

import com.print3dmanager.erp.printer.dto.PrinterConfigRequest;
import com.print3dmanager.erp.printer.dto.PrinterConfigResponse;
import com.print3dmanager.erp.printer.dto.PrinterCreateRequest;
import com.print3dmanager.erp.printer.dto.PrinterResponse;
import com.print3dmanager.erp.printer.dto.PrinterUpdateRequest;
import com.print3dmanager.erp.printer.model.Printer;
import com.print3dmanager.erp.printer.model.PrinterConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Conversões entidade ↔ DTO de impressoras e configurações de custo.
 */
@Mapper
public interface PrinterMapper {

    PrinterResponse toResponse(Printer printer);

    Printer toEntity(PrinterCreateRequest request);

    void atualizar(@MappingTarget Printer printer, PrinterUpdateRequest request);

    @Mapping(target = "id", source = "config.id")
    @Mapping(target = "impressoraId", source = "config.impressora.id")
    @Mapping(target = "criadoEm", source = "config.criadoEm")
    @Mapping(target = "atualizadoEm", source = "config.atualizadoEm")
    PrinterConfigResponse toConfigResponse(PrinterConfiguration config, String origem);

    void atualizarConfig(@MappingTarget PrinterConfiguration config, PrinterConfigRequest request);
}
