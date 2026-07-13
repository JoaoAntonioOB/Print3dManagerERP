package com.print3dmanager.erp.order.controller;

import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.order.dto.PrintCompleteRequest;
import com.print3dmanager.erp.order.dto.PrintFailRequest;
import com.print3dmanager.erp.order.dto.PrintHistoryResponse;
import com.print3dmanager.erp.order.dto.PrintStartRequest;
import com.print3dmanager.erp.order.model.PrintStatus;
import com.print3dmanager.erp.order.service.PrintHistoryService;
import com.print3dmanager.erp.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Histórico de impressões (jobs): ADMINISTRADOR e OPERADOR gerenciam;
 * FINANCEIRO e VISUALIZADOR apenas consultam.
 */
@RestController
@RequestMapping("/prints")
@RequiredArgsConstructor
@Tag(name = "Impressões", description = "Registro dos jobs de impressão executados")
public class PrintHistoryController {

    private static final String PODE_GERENCIAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR')";
    private static final String PODE_CONSULTAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR')";

    private final PrintHistoryService printHistoryService;

    @GetMapping
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Lista jobs de impressão com paginação e filtros opcionais")
    public PageResponse<PrintHistoryResponse> listar(
            @RequestParam(required = false) Long impressoraId,
            @RequestParam(required = false) PrintStatus status,
            @RequestParam(required = false) Long itemPedidoId,
            @Parameter(description = "Início do período (iniciadoEm ≥ de), ISO-8601 UTC")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @Parameter(description = "Fim do período (iniciadoEm ≤ ate), ISO-8601 UTC")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @ParameterObject
            @PageableDefault(sort = "iniciadoEm", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return printHistoryService.listar(impressoraId, status, itemPedidoId, de, ate, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Busca um job de impressão pelo id")
    public PrintHistoryResponse buscarPorId(@PathVariable Long id) {
        return printHistoryService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inicia um job de impressão",
            description = "A impressora precisa estar DISPONIVEL e passa a IMPRIMINDO. "
                    + "Item de pedido, se informado, precisa estar em pedido EM_PRODUCAO.")
    public PrintHistoryResponse iniciar(@Valid @RequestBody PrintStartRequest request,
                                        @AuthenticationPrincipal SecurityUser usuario) {
        return printHistoryService.iniciar(request, usuario.getUser().getId());
    }

    @PatchMapping("/{id}/concluir")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Conclui um job EM_ANDAMENTO",
            description = "Libera a impressora, soma horas de uso, abate o peso informado do "
                    + "estoque do filamento e calcula o custo real do job.")
    public PrintHistoryResponse concluir(@PathVariable Long id,
                                         @Valid @RequestBody PrintCompleteRequest request) {
        return printHistoryService.concluir(id, request);
    }

    @PatchMapping("/{id}/falhar")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Registra a falha de um job EM_ANDAMENTO",
            description = "O material desperdiçado informado também é abatido do estoque.")
    public PrintHistoryResponse falhar(@PathVariable Long id,
                                       @Valid @RequestBody PrintFailRequest request) {
        return printHistoryService.falhar(id, request);
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Cancela um job EM_ANDAMENTO (corpo opcional)")
    public PrintHistoryResponse cancelar(@PathVariable Long id,
                                         @Valid @RequestBody(required = false)
                                         PrintCompleteRequest request) {
        return printHistoryService.cancelar(id, request);
    }
}
