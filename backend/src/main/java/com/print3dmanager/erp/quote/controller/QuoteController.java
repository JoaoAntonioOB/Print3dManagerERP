package com.print3dmanager.erp.quote.controller;

import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.order.dto.OrderResponse;
import com.print3dmanager.erp.quote.dto.QuoteCreateRequest;
import com.print3dmanager.erp.quote.dto.QuoteResponse;
import com.print3dmanager.erp.quote.dto.QuoteStatusRequest;
import com.print3dmanager.erp.quote.dto.QuoteUpdateRequest;
import com.print3dmanager.erp.quote.model.QuoteStatus;
import com.print3dmanager.erp.quote.service.QuoteService;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * Orçamentos: ADMINISTRADOR e OPERADOR gerenciam;
 * FINANCEIRO e VISUALIZADOR apenas consultam.
 */
@RestController
@RequestMapping("/quotes")
@RequiredArgsConstructor
@Tag(name = "Orçamentos", description = "Orçamentos com precificação por custos + markup "
        + "e link público de aprovação")
public class QuoteController {

    private static final String PODE_GERENCIAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR')";
    private static final String PODE_CONSULTAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR')";

    private final QuoteService quoteService;

    @GetMapping
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Lista orçamentos com paginação e filtros opcionais")
    public PageResponse<QuoteResponse> listar(
            @Parameter(description = "Busca por número do orçamento ou nome do cliente")
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) QuoteStatus status,
            @RequestParam(required = false) Long clienteId,
            @ParameterObject
            @PageableDefault(sort = "criadoEm", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return quoteService.listar(busca, status, clienteId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Busca um orçamento pelo id (visão interna, com custos)")
    public QuoteResponse buscarPorId(@PathVariable Long id) {
        return quoteService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um orçamento",
            description = "Número gerado automaticamente (ORC-<ano>-<sequencial>). Custos "
                    + "decompostos e preço sugerido calculados pela estratégia de precificação: "
                    + "filamento + energia + hora máquina + desgaste + markup.")
    public QuoteResponse criar(@Valid @RequestBody QuoteCreateRequest request,
                               @AuthenticationPrincipal SecurityUser usuario) {
        return quoteService.criar(request, usuario.getUser().getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Atualiza um orçamento em RASCUNHO (custos recalculados)")
    public QuoteResponse atualizar(@PathVariable Long id,
                                   @Valid @RequestBody QuoteUpdateRequest request) {
        return quoteService.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Altera o status do orçamento",
            description = "RASCUNHO→ENVIADO; ENVIADO→RASCUNHO|APROVADO|REJEITADO|EXPIRADO. "
                    + "CONVERTIDO apenas via POST /quotes/{id}/converter.")
    public QuoteResponse alterarStatus(@PathVariable Long id,
                                       @Valid @RequestBody QuoteStatusRequest request) {
        return quoteService.alterarStatus(id, request);
    }

    @PostMapping("/{id}/converter")
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Converte um orçamento APROVADO em pedido",
            description = "Cria o pedido com um item espelhando o orçamento (preço efetivo, "
                    + "peso, tempo, filamento) e marca o orçamento como CONVERTIDO.")
    public OrderResponse converter(@PathVariable Long id,
                                   @AuthenticationPrincipal SecurityUser usuario) {
        return quoteService.converter(id, usuario.getUser().getId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um orçamento em RASCUNHO")
    public void excluir(@PathVariable Long id) {
        quoteService.excluir(id);
    }
}
