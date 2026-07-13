package com.print3dmanager.erp.printer.controller;

import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.printer.dto.PrinterCreateRequest;
import com.print3dmanager.erp.printer.dto.PrinterResponse;
import com.print3dmanager.erp.printer.dto.PrinterStatusRequest;
import com.print3dmanager.erp.printer.dto.PrinterUpdateRequest;
import com.print3dmanager.erp.printer.model.PrinterStatus;
import com.print3dmanager.erp.printer.service.PrinterService;
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
 * Gestão do parque de impressoras: ADMINISTRADOR e OPERADOR gerenciam;
 * FINANCEIRO e VISUALIZADOR apenas consultam.
 */
@RestController
@RequestMapping("/printers")
@RequiredArgsConstructor
@Tag(name = "Impressoras", description = "Gestão do parque de impressoras 3D")
public class PrinterController {

    private static final String PODE_GERENCIAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR')";
    private static final String PODE_CONSULTAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR')";

    private final PrinterService printerService;

    @GetMapping
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Lista impressoras com paginação e filtros opcionais")
    public PageResponse<PrinterResponse> listar(
            @Parameter(description = "Busca por nome, marca ou modelo")
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) PrinterStatus status,
            @RequestParam(required = false) Boolean ativo,
            @ParameterObject @PageableDefault(sort = "nome") Pageable pageable) {
        return printerService.listar(busca, status, ativo, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Busca uma impressora pelo id")
    public PrinterResponse buscarPorId(@PathVariable Long id) {
        return printerService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma nova impressora")
    public PrinterResponse criar(@Valid @RequestBody PrinterCreateRequest request) {
        return printerService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Atualiza os dados de uma impressora")
    public PrinterResponse atualizar(@PathVariable Long id,
                                     @Valid @RequestBody PrinterUpdateRequest request) {
        return printerService.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Altera a situação operacional da impressora",
            description = "DISPONIVEL, IMPRIMINDO, EM_MANUTENCAO ou INATIVA. "
                    + "Impressoras desativadas não podem mudar de status.")
    public PrinterResponse alterarStatus(@PathVariable Long id,
                                         @Valid @RequestBody PrinterStatusRequest request) {
        return printerService.alterarStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa uma impressora (soft delete)")
    public void desativar(@PathVariable Long id) {
        printerService.desativar(id);
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Reativa uma impressora desativada (volta como DISPONIVEL)")
    public PrinterResponse reativar(@PathVariable Long id) {
        return printerService.reativar(id);
    }
}
