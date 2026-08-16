package com.print3dmanager.erp.filament.service;

import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.common.model.StockMovementType;
import com.print3dmanager.erp.filament.dto.FilamentResponse;
import com.print3dmanager.erp.filament.dto.FilamentStockRequest;
import com.print3dmanager.erp.filament.mapper.FilamentMapper;
import com.print3dmanager.erp.filament.model.Filament;
import com.print3dmanager.erp.filament.model.FilamentMaterial;
import com.print3dmanager.erp.filament.repository.FilamentRepository;
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
 * Regras de negócio de {@link FilamentService}: movimentação de estoque em
 * gramas com leitura via lock pessimista (ENTRADA soma, SAIDA subtrai,
 * saldo negativo rejeitado, filamento desativado não movimenta) e soft
 * delete (desativar/reativar).
 */
@ExtendWith(MockitoExtension.class)
class FilamentServiceTest {

    @Mock
    private FilamentRepository filamentRepository;
    @Mock
    private FilamentMapper filamentMapper;

    @InjectMocks
    private FilamentService service;

    @Test
    @DisplayName("movimentarEstoque: ENTRADA soma ao saldo atual, lido com lock pessimista")
    void entradaSomaAoSaldo() {
        Filament filamento = filamento(1L, "500.00", true);
        when(filamentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(filamento));
        when(filamentMapper.toResponse(filamento)).thenReturn(resposta());

        service.movimentarEstoque(1L,
                new FilamentStockRequest(StockMovementType.ENTRADA, new BigDecimal("250.00")));

        assertThat(filamento.getQuantidadeEstoqueG()).isEqualByComparingTo("750.00");
    }

    @Test
    @DisplayName("movimentarEstoque: SAIDA subtrai do saldo atual")
    void saidaSubtraiDoSaldo() {
        Filament filamento = filamento(1L, "500.00", true);
        when(filamentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(filamento));
        when(filamentMapper.toResponse(filamento)).thenReturn(resposta());

        service.movimentarEstoque(1L,
                new FilamentStockRequest(StockMovementType.SAIDA, new BigDecimal("200.00")));

        assertThat(filamento.getQuantidadeEstoqueG()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("movimentarEstoque: SAIDA maior que o saldo é rejeitada, saldo intacto")
    void saidaMaiorQueSaldoRejeitada() {
        Filament filamento = filamento(1L, "100.00", true);
        when(filamentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(filamento));

        assertThatThrownBy(() -> service.movimentarEstoque(1L,
                new FilamentStockRequest(StockMovementType.SAIDA, new BigDecimal("150.00"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("insuficiente");
        assertThat(filamento.getQuantidadeEstoqueG()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("movimentarEstoque: filamento desativado não pode ser movimentado")
    void filamentoDesativadoNaoMovimenta() {
        Filament filamento = filamento(1L, "500.00", false);
        when(filamentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(filamento));

        assertThatThrownBy(() -> service.movimentarEstoque(1L,
                new FilamentStockRequest(StockMovementType.ENTRADA, new BigDecimal("10.00"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desativado");
    }

    @Test
    @DisplayName("movimentarEstoque: filamento inexistente → 404")
    void movimentarFilamentoInexistente() {
        when(filamentRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.movimentarEstoque(1L,
                new FilamentStockRequest(StockMovementType.ENTRADA, new BigDecimal("10.00"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("desativar: soft delete marca ativo = false, preservando o histórico de uso")
    void desativarMarcaInativo() {
        Filament filamento = filamento(1L, "500.00", true);
        when(filamentRepository.findById(1L)).thenReturn(Optional.of(filamento));

        service.desativar(1L);

        assertThat(filamento.isAtivo()).isFalse();
    }

    @Test
    @DisplayName("reativar: volta ativo = true")
    void reativarMarcaAtivo() {
        Filament filamento = filamento(1L, "500.00", false);
        when(filamentRepository.findById(1L)).thenReturn(Optional.of(filamento));
        when(filamentMapper.toResponse(filamento)).thenReturn(resposta());

        service.reativar(1L);

        assertThat(filamento.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("buscarPorId: id inexistente → 404")
    void buscarPorIdInexistente() {
        when(filamentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== helpers =====

    private Filament filamento(Long id, String quantidadeG, boolean ativo) {
        Filament filamento = new Filament();
        filamento.setId(id);
        filamento.setNome("PLA Preto");
        filamento.setMaterial(FilamentMaterial.PLA);
        filamento.setCustoPorKg(new BigDecimal("80.00"));
        filamento.setQuantidadeEstoqueG(new BigDecimal(quantidadeG));
        filamento.setEstoqueMinimoG(BigDecimal.ZERO);
        filamento.setAtivo(ativo);
        return filamento;
    }

    private FilamentResponse resposta() {
        return new FilamentResponse(1L, "PLA Preto", null, FilamentMaterial.PLA, null,
                new BigDecimal("1.75"), null, new BigDecimal("80.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, false, null, null, true, Instant.now(), Instant.now());
    }
}
