package com.print3dmanager.erp.inventory.service;

import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.common.model.StockMovementType;
import com.print3dmanager.erp.inventory.dto.InventoryItemResponse;
import com.print3dmanager.erp.inventory.dto.InventoryStockRequest;
import com.print3dmanager.erp.inventory.mapper.InventoryItemMapper;
import com.print3dmanager.erp.inventory.model.InventoryItem;
import com.print3dmanager.erp.inventory.repository.InventoryItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Regras de negócio de {@link InventoryItemService}: movimentação de
 * estoque (ENTRADA soma, SAIDA subtrai, saldo negativo rejeitado, item
 * desativado não movimenta) e soft delete (desativar/reativar).
 */
@ExtendWith(MockitoExtension.class)
class InventoryItemServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @Mock
    private InventoryItemMapper inventoryItemMapper;

    @InjectMocks
    private InventoryItemService service;

    @Test
    @DisplayName("movimentarEstoque: ENTRADA soma ao saldo atual")
    void entradaSomaAoSaldo() {
        InventoryItem item = item(1L, "10.000", true);
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(inventoryItemMapper.toResponse(item)).thenReturn(resposta());

        service.movimentarEstoque(1L,
                new InventoryStockRequest(StockMovementType.ENTRADA, new BigDecimal("5.000")));

        assertThat(item.getQuantidade()).isEqualByComparingTo("15.000");
    }

    @Test
    @DisplayName("movimentarEstoque: SAIDA subtrai do saldo atual")
    void saidaSubtraiDoSaldo() {
        InventoryItem item = item(1L, "10.000", true);
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(inventoryItemMapper.toResponse(item)).thenReturn(resposta());

        service.movimentarEstoque(1L,
                new InventoryStockRequest(StockMovementType.SAIDA, new BigDecimal("4.000")));

        assertThat(item.getQuantidade()).isEqualByComparingTo("6.000");
    }

    @Test
    @DisplayName("movimentarEstoque: SAIDA maior que o saldo é rejeitada, saldo intacto")
    void saidaMaiorQueSaldoRejeitada() {
        InventoryItem item = item(1L, "3.000", true);
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.movimentarEstoque(1L,
                new InventoryStockRequest(StockMovementType.SAIDA, new BigDecimal("5.000"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("insuficiente");
        assertThat(item.getQuantidade()).isEqualByComparingTo("3.000");
    }

    @Test
    @DisplayName("movimentarEstoque: SAIDA que zera exatamente o saldo é aceita (borda)")
    void saidaQueZeraSaldoAceita() {
        InventoryItem item = item(1L, "5.000", true);
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(inventoryItemMapper.toResponse(item)).thenReturn(resposta());

        service.movimentarEstoque(1L,
                new InventoryStockRequest(StockMovementType.SAIDA, new BigDecimal("5.000")));

        assertThat(item.getQuantidade()).isEqualByComparingTo("0.000");
    }

    @Test
    @DisplayName("movimentarEstoque: item desativado não pode ser movimentado")
    void itemDesativadoNaoMovimenta() {
        InventoryItem item = item(1L, "10.000", false);
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.movimentarEstoque(1L,
                new InventoryStockRequest(StockMovementType.ENTRADA, new BigDecimal("5.000"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desativado");
    }

    @Test
    @DisplayName("movimentarEstoque: item inexistente → 404")
    void movimentarItemInexistente() {
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.movimentarEstoque(1L,
                new InventoryStockRequest(StockMovementType.ENTRADA, new BigDecimal("1"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("desativar: soft delete marca ativo = false, preservando o item")
    void desativarMarcaInativo() {
        InventoryItem item = item(1L, "10.000", true);
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));

        service.desativar(1L);

        assertThat(item.isAtivo()).isFalse();
    }

    @Test
    @DisplayName("reativar: volta ativo = true")
    void reativarMarcaAtivo() {
        InventoryItem item = item(1L, "10.000", false);
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(inventoryItemMapper.toResponse(item)).thenReturn(resposta());

        service.reativar(1L);

        assertThat(item.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("buscarPorId: id inexistente → 404")
    void buscarPorIdInexistente() {
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== helpers =====

    private InventoryItem item(Long id, String quantidade, boolean ativo) {
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setNome("Parafuso M3");
        item.setUnidadeMedida("UN");
        item.setQuantidade(new BigDecimal(quantidade));
        item.setQuantidadeMinima(BigDecimal.ZERO);
        item.setAtivo(ativo);
        return item;
    }

    private InventoryItemResponse resposta() {
        return new InventoryItemResponse(1L, "Parafuso M3", null, null, BigDecimal.ZERO, "UN",
                BigDecimal.ZERO, false, null, null, true, Instant.now(), Instant.now());
    }
}
