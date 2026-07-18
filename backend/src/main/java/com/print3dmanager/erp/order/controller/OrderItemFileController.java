package com.print3dmanager.erp.order.controller;

import com.print3dmanager.erp.order.dto.ModelFileDownload;
import com.print3dmanager.erp.order.dto.OrderItemResponse;
import com.print3dmanager.erp.order.service.OrderItemFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * Arquivo de modelo 3D (STL/3MF) de um item de pedido. Mesmos perfis do
 * módulo de pedidos: ADMINISTRADOR e OPERADOR anexam/removem (pedido
 * PENDENTE); todos os perfis internos baixam.
 */
@RestController
@RequestMapping("/orders/{pedidoId}/itens/{itemId}/arquivo")
@RequiredArgsConstructor
@Tag(name = "Pedidos")
public class OrderItemFileController {

    private static final String PODE_GERENCIAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR')";
    private static final String PODE_CONSULTAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR')";

    private final OrderItemFileService orderItemFileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(PODE_GERENCIAR)
    @Operation(summary = "Anexa (ou substitui) o arquivo STL/3MF do item",
            description = "Multipart com a parte 'arquivo'. Somente pedidos PENDENTES; "
                    + "extensões aceitas: .stl e .3mf; limite de 100 MB por arquivo. "
                    + "Um novo upload substitui o arquivo anterior.")
    public OrderItemResponse anexar(@PathVariable Long pedidoId,
                                    @PathVariable Long itemId,
                                    @Parameter(description = "Arquivo do modelo 3D")
                                    @RequestPart("arquivo") MultipartFile arquivo) {
        return orderItemFileService.anexar(pedidoId, itemId, arquivo);
    }

    @GetMapping
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Baixa o arquivo de modelo do item",
            description = "Download com o nome original (sanitizado); disponível em "
                    + "qualquer status do pedido. Item sem arquivo: 404.")
    public ResponseEntity<Resource> baixar(@PathVariable Long pedidoId,
                                           @PathVariable Long itemId) {
        ModelFileDownload download = orderItemFileService.baixar(pedidoId, itemId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.tamanhoBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.nomeArquivo(), StandardCharsets.UTF_8)
                        .build().toString())
                .body(download.recurso());
    }

    @DeleteMapping
    @PreAuthorize(PODE_GERENCIAR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove o arquivo de modelo do item",
            description = "Apaga do armazenamento e limpa a referência. "
                    + "Somente pedidos PENDENTES; item sem arquivo: 404.")
    public void remover(@PathVariable Long pedidoId, @PathVariable Long itemId) {
        orderItemFileService.remover(pedidoId, itemId);
    }
}
