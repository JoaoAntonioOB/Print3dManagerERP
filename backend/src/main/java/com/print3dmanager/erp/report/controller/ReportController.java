package com.print3dmanager.erp.report.controller;

import com.print3dmanager.erp.order.model.OrderStatus;
import com.print3dmanager.erp.report.dto.PdfReportFile;
import com.print3dmanager.erp.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Relatórios em PDF. Pedidos e consumo de filamento são acessíveis a todos
 * os perfis internos; o financeiro é restrito a ADMINISTRADOR e FINANCEIRO.
 * Sem período informado, todos consideram o mês corrente (UTC).
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Relatórios", description = "Relatórios em PDF para download")
public class ReportController {

    private static final String PODE_CONSULTAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR')";
    private static final String PODE_VER_FINANCEIRO =
            "hasAnyRole('ADMINISTRADOR', 'FINANCEIRO')";

    private final ReportService reportService;

    @GetMapping(value = "/pedidos", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Relatório de pedidos do período (PDF)",
            description = "Pedidos abertos no período (data de criação, UTC), com totais. "
                    + "Filtro opcional por status.")
    public ResponseEntity<byte[]> pedidos(
            @Parameter(description = "Início do período (padrão: 1º dia do mês corrente)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Fim do período (padrão: último dia do mês corrente)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) OrderStatus status) {
        return download(reportService.pedidos(de, ate, status));
    }

    @GetMapping(value = "/financeiro", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize(PODE_VER_FINANCEIRO)
    @Operation(summary = "Relatório financeiro do período (PDF)",
            description = "Transações do período (canceladas identificadas) e o resumo de "
                    + "receitas × despesas × saldos. Restrito a ADMINISTRADOR e FINANCEIRO.")
    public ResponseEntity<byte[]> financeiro(
            @Parameter(description = "Início do período (padrão: 1º dia do mês corrente)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Fim do período (padrão: último dia do mês corrente)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return download(reportService.financeiro(de, ate));
    }

    @GetMapping(value = "/consumo-filamento", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Relatório de consumo de filamento do período (PDF)",
            description = "Consumo agregado por filamento nas impressões finalizadas no "
                    + "período com peso registrado, com totais de gramas e custo.")
    public ResponseEntity<byte[]> consumoFilamento(
            @Parameter(description = "Início do período (padrão: 1º dia do mês corrente)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Fim do período (padrão: último dia do mês corrente)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return download(reportService.consumoFilamento(de, ate));
    }

    private ResponseEntity<byte[]> download(PdfReportFile relatorio) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(relatorio.nomeArquivo())
                        .build()
                        .toString())
                .body(relatorio.conteudo());
    }
}
