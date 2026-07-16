package com.print3dmanager.erp.financial.controller;

import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.financial.dto.FinancialSummaryResponse;
import com.print3dmanager.erp.financial.dto.FinancialTransactionCreateRequest;
import com.print3dmanager.erp.financial.dto.FinancialTransactionResponse;
import com.print3dmanager.erp.financial.dto.FinancialTransactionStatusRequest;
import com.print3dmanager.erp.financial.dto.FinancialTransactionUpdateRequest;
import com.print3dmanager.erp.financial.dto.MonthlyFinancialPoint;
import com.print3dmanager.erp.financial.model.TransactionStatus;
import com.print3dmanager.erp.financial.model.TransactionType;
import com.print3dmanager.erp.financial.service.FinancialTransactionService;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Transações financeiras e resumos: ADMINISTRADOR e FINANCEIRO gerenciam;
 * OPERADOR e VISUALIZADOR apenas consultam.
 */
@RestController
@RequestMapping("/financial")
@RequiredArgsConstructor
@Tag(name = "Financeiro", description = "Receitas, despesas e resumos financeiros")
public class FinancialTransactionController {

    private static final String PODE_GERENCIAR =
            "hasAnyRole('ADMINISTRADOR', 'FINANCEIRO')";
    private static final String PODE_CONSULTAR =
            "hasAnyRole('ADMINISTRADOR', 'FINANCEIRO', 'OPERADOR', 'VISUALIZADOR')";

    private final FinancialTransactionService financialTransactionService;

    @GetMapping("/transactions")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Lista transações com paginação e filtros opcionais")
    public PageResponse<FinancialTransactionResponse> listar(
            @Parameter(description = "Busca por descrição, categoria ou observações")
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) TransactionType tipo,
            @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "Categoria exata (sem diferenciar maiúsculas)")
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Long pedidoId,
            @RequestParam(required = false) Long clienteId,
            @Parameter(description = "Início do período (dataTransacao ≥ de), ISO-8601")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Fim do período (dataTransacao ≤ ate), ISO-8601")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @ParameterObject
            @PageableDefault(sort = "dataTransacao", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return financialTransactionService.listar(busca, tipo, status, categoria, pedidoId,
                clienteId, de, ate, pageable);
    }

    @GetMapping("/transactions/{id}")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Busca uma transação pelo id")
    public FinancialTransactionResponse buscarPorId(@PathVariable Long id) {
        return financialTransactionService.buscarPorId(id);
    }

    @PostMapping("/transactions")
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Lança uma transação (receita ou despesa)",
            description = "Status omitido assume PENDENTE; CANCELADA não é aceita no cadastro. "
                    + "Cliente omitido com pedido informado herda o cliente do pedido.")
    public FinancialTransactionResponse criar(
            @Valid @RequestBody FinancialTransactionCreateRequest request) {
        return financialTransactionService.criar(request);
    }

    @PutMapping("/transactions/{id}")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Atualiza uma transação PENDENTE",
            description = "Transações PAGAS devem ser estornadas (status PENDENTE) antes de "
                    + "editar; o status muda apenas pelo endpoint de status.")
    public FinancialTransactionResponse atualizar(@PathVariable Long id,
            @Valid @RequestBody FinancialTransactionUpdateRequest request) {
        return financialTransactionService.atualizar(id, request);
    }

    @PatchMapping("/transactions/{id}/status")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Altera a situação da transação seguindo o ciclo de vida",
            description = "PENDENTE→PAGA|CANCELADA, PAGA→PENDENTE (estorno da baixa); "
                    + "CANCELADA é terminal. Demais transições: 400.")
    public FinancialTransactionResponse alterarStatus(@PathVariable Long id,
            @Valid @RequestBody FinancialTransactionStatusRequest request) {
        return financialTransactionService.alterarStatus(id, request.status());
    }

    @DeleteMapping("/transactions/{id}")
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma transação PENDENTE",
            description = "Transações pagas ou canceladas são preservadas como histórico — "
                    + "cancele em vez de excluir.")
    public void excluir(@PathVariable Long id) {
        financialTransactionService.excluir(id);
    }

    @GetMapping("/resumo")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Resumo do período: receitas × despesas × saldos",
            description = "Sem período informado, considera o mês corrente (UTC). "
                    + "Transações CANCELADAS ficam de fora.")
    public FinancialSummaryResponse resumo(
            @Parameter(description = "Início do período (padrão: 1º dia do mês corrente)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Fim do período (padrão: último dia do mês corrente)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return financialTransactionService.resumo(de, ate);
    }

    @GetMapping("/resumo/mensal")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Série mensal do movimento realizado (transações PAGAS)",
            description = "Sempre devolve os N meses completos (1–60, incluindo o corrente), "
                    + "com meses sem movimento zerados, em ordem cronológica.")
    public List<MonthlyFinancialPoint> resumoMensal(
            @Parameter(description = "Quantidade de meses da série (1–60)")
            @RequestParam(defaultValue = "12") int meses) {
        return financialTransactionService.resumoMensal(meses);
    }
}
