package com.print3dmanager.erp.order.controller;

import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.order.dto.OrderCreateRequest;
import com.print3dmanager.erp.order.dto.OrderResponse;
import com.print3dmanager.erp.order.dto.OrderStatusRequest;
import com.print3dmanager.erp.order.dto.OrderSummaryResponse;
import com.print3dmanager.erp.order.dto.OrderUpdateRequest;
import com.print3dmanager.erp.order.model.OrderStatus;
import com.print3dmanager.erp.order.service.OrderService;
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
 * Pedidos de produção: ADMINISTRADOR e OPERADOR gerenciam;
 * FINANCEIRO e VISUALIZADOR apenas consultam.
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Pedidos de produção com itens (peças)")
public class OrderController {

    private static final String PODE_GERENCIAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR')";
    private static final String PODE_CONSULTAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR')";

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Lista pedidos (resumo, sem itens) com paginação e filtros opcionais")
    public PageResponse<OrderSummaryResponse> listar(
            @Parameter(description = "Busca por número do pedido ou nome do cliente")
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long clienteId,
            @ParameterObject
            @PageableDefault(sort = "criadoEm", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return orderService.listar(busca, status, clienteId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Busca um pedido pelo id, com itens")
    public OrderResponse buscarPorId(@PathVariable Long id) {
        return orderService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre um novo pedido",
            description = "Número gerado automaticamente (PED-<ano>-<sequencial>); "
                    + "o valor total é calculado dos itens menos o desconto.")
    public OrderResponse criar(@Valid @RequestBody OrderCreateRequest request,
                               @AuthenticationPrincipal SecurityUser usuario) {
        return orderService.criar(request, usuario.getUser().getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Atualiza um pedido PENDENTE (dados e itens)",
            description = "A lista de itens substitui a atual: com id atualiza, sem id cria, "
                    + "ausentes são removidos. Pedidos fora de PENDENTE não podem ser editados.")
    public OrderResponse atualizar(@PathVariable Long id,
                                   @Valid @RequestBody OrderUpdateRequest request) {
        return orderService.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Altera o status do pedido seguindo o ciclo de vida",
            description = "PENDENTE→EM_PRODUCAO|CANCELADO, EM_PRODUCAO→CONCLUIDO|CANCELADO, "
                    + "CONCLUIDO→ENTREGUE (registra a data de entrega). Demais transições: 400.")
    public OrderResponse alterarStatus(@PathVariable Long id,
                                       @Valid @RequestBody OrderStatusRequest request) {
        return orderService.alterarStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um pedido PENDENTE (itens saem em cascata)",
            description = "Pedidos que já entraram em produção devem ser CANCELADOS, "
                    + "preservando o histórico.")
    public void excluir(@PathVariable Long id) {
        orderService.excluir(id);
    }
}
