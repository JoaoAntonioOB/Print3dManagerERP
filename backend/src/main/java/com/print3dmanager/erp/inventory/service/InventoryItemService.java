package com.print3dmanager.erp.inventory.service;

import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.common.model.StockMovementType;
import com.print3dmanager.erp.inventory.dto.InventoryItemCreateRequest;
import com.print3dmanager.erp.inventory.dto.InventoryItemResponse;
import com.print3dmanager.erp.inventory.dto.InventoryItemUpdateRequest;
import com.print3dmanager.erp.inventory.dto.InventoryStockRequest;
import com.print3dmanager.erp.inventory.mapper.InventoryItemMapper;
import com.print3dmanager.erp.inventory.model.InventoryItem;
import com.print3dmanager.erp.inventory.repository.InventoryItemRepository;
import com.print3dmanager.erp.inventory.repository.InventoryItemSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Regras de negócio de itens de estoque (insumos gerais): CRUD com soft
 * delete e movimentação manual na unidade de medida do item.
 */
@Service
@RequiredArgsConstructor
public class InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryItemMapper inventoryItemMapper;

    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> listar(String busca, String categoria,
                                                      Boolean ativo, Boolean estoqueBaixo,
                                                      Pageable pageable) {
        return PageResponse.de(
                inventoryItemRepository.findAll(
                                InventoryItemSpecifications.comFiltros(busca, categoria, ativo,
                                        estoqueBaixo), pageable)
                        .map(inventoryItemMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse buscarPorId(Long id) {
        return inventoryItemMapper.toResponse(obterItem(id));
    }

    @Transactional
    public InventoryItemResponse criar(InventoryItemCreateRequest request) {
        InventoryItem item = inventoryItemMapper.toEntity(request);
        return inventoryItemMapper.toResponse(inventoryItemRepository.save(item));
    }

    @Transactional
    public InventoryItemResponse atualizar(Long id, InventoryItemUpdateRequest request) {
        InventoryItem item = obterItem(id);
        inventoryItemMapper.atualizar(item, request);
        return inventoryItemMapper.toResponse(item);
    }

    @Transactional
    public InventoryItemResponse movimentarEstoque(Long id, InventoryStockRequest request) {
        InventoryItem item = obterItem(id);
        if (!item.isAtivo()) {
            throw new BusinessException(
                    "Não é possível movimentar o estoque de um item desativado.");
        }

        BigDecimal saldoAtual = item.getQuantidade();
        BigDecimal novoSaldo = request.tipo() == StockMovementType.ENTRADA
                ? saldoAtual.add(request.quantidade())
                : saldoAtual.subtract(request.quantidade());

        if (novoSaldo.signum() < 0) {
            throw new BusinessException(
                    "Saldo insuficiente: o estoque atual é de %s %s e a saída solicitada é de %s."
                            .formatted(saldoAtual.stripTrailingZeros().toPlainString(),
                                    item.getUnidadeMedida(),
                                    request.quantidade().stripTrailingZeros().toPlainString()));
        }

        item.setQuantidade(novoSaldo);
        return inventoryItemMapper.toResponse(item);
    }

    /** Soft delete: preserva o histórico de consumo do insumo. */
    @Transactional
    public void desativar(Long id) {
        InventoryItem item = obterItem(id);
        item.setAtivo(false);
    }

    @Transactional
    public InventoryItemResponse reativar(Long id) {
        InventoryItem item = obterItem(id);
        item.setAtivo(true);
        return inventoryItemMapper.toResponse(item);
    }

    private InventoryItem obterItem(Long id) {
        return inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item de estoque", id));
    }
}
