package com.print3dmanager.erp.printer.controller;

import com.print3dmanager.erp.printer.dto.PrinterConfigRequest;
import com.print3dmanager.erp.printer.dto.PrinterConfigResponse;
import com.print3dmanager.erp.printer.service.PrinterConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configurações de custo dos orçamentos. Escrita restrita ao
 * ADMINISTRADOR (impacta a precificação de toda a empresa).
 */
@RestController
@RequestMapping("/printers")
@RequiredArgsConstructor
@Tag(name = "Configurações de custo",
        description = "Parâmetros do cálculo de orçamentos (global e por impressora)")
public class PrinterConfigurationController {

    private static final String SOMENTE_ADMIN = "hasRole('ADMINISTRADOR')";
    private static final String PODE_CONSULTAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR')";

    private final PrinterConfigurationService configService;

    @GetMapping("/config")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Consulta a configuração global de custos")
    public PrinterConfigResponse buscarGlobal() {
        return configService.buscarGlobal();
    }

    @PutMapping("/config")
    @PreAuthorize(SOMENTE_ADMIN)
    @Operation(summary = "Cria ou atualiza a configuração global de custos")
    public PrinterConfigResponse salvarGlobal(@Valid @RequestBody PrinterConfigRequest request) {
        return configService.salvarGlobal(request);
    }

    @GetMapping("/{id}/config")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Consulta a configuração efetiva de uma impressora",
            description = "Retorna a configuração própria da impressora, se existir; "
                    + "caso contrário, a global (campo origem indica qual).")
    public PrinterConfigResponse buscarEfetiva(@PathVariable Long id) {
        return configService.buscarEfetiva(id);
    }

    @PutMapping("/{id}/config")
    @PreAuthorize(SOMENTE_ADMIN)
    @Operation(summary = "Cria ou atualiza a configuração própria de uma impressora")
    public PrinterConfigResponse salvarDaImpressora(@PathVariable Long id,
                                                    @Valid @RequestBody PrinterConfigRequest request) {
        return configService.salvarDaImpressora(id, request);
    }

    @DeleteMapping("/{id}/config")
    @PreAuthorize(SOMENTE_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a configuração própria (volta a valer a global)")
    public void removerDaImpressora(@PathVariable Long id) {
        configService.removerDaImpressora(id);
    }
}
