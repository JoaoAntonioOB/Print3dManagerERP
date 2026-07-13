package com.print3dmanager.erp.filament.service;

import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.filament.dto.FilamentCreateRequest;
import com.print3dmanager.erp.filament.dto.FilamentResponse;
import com.print3dmanager.erp.filament.dto.FilamentStockRequest;
import com.print3dmanager.erp.filament.dto.FilamentUpdateRequest;
import com.print3dmanager.erp.filament.mapper.FilamentMapper;
import com.print3dmanager.erp.filament.model.Filament;
import com.print3dmanager.erp.filament.model.FilamentMaterial;
import com.print3dmanager.erp.filament.model.StockMovementType;
import com.print3dmanager.erp.filament.repository.FilamentRepository;
import com.print3dmanager.erp.filament.repository.FilamentSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Regras de negócio de filamentos: CRUD com soft delete e movimentação
 * manual de estoque em gramas (o consumo automático virá com os pedidos).
 */
@Service
@RequiredArgsConstructor
public class FilamentService {

    private final FilamentRepository filamentRepository;
    private final FilamentMapper filamentMapper;

    @Transactional(readOnly = true)
    public PageResponse<FilamentResponse> listar(String busca, FilamentMaterial material,
                                                 Boolean ativo, Boolean estoqueBaixo,
                                                 Pageable pageable) {
        return PageResponse.de(
                filamentRepository.findAll(
                                FilamentSpecifications.comFiltros(busca, material, ativo,
                                        estoqueBaixo), pageable)
                        .map(filamentMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public FilamentResponse buscarPorId(Long id) {
        return filamentMapper.toResponse(obterFilamento(id));
    }

    @Transactional
    public FilamentResponse criar(FilamentCreateRequest request) {
        Filament filamento = filamentMapper.toEntity(request);
        return filamentMapper.toResponse(filamentRepository.save(filamento));
    }

    @Transactional
    public FilamentResponse atualizar(Long id, FilamentUpdateRequest request) {
        Filament filamento = obterFilamento(id);
        filamentMapper.atualizar(filamento, request);
        return filamentMapper.toResponse(filamento);
    }

    @Transactional
    public FilamentResponse movimentarEstoque(Long id, FilamentStockRequest request) {
        Filament filamento = obterFilamento(id);
        if (!filamento.isAtivo()) {
            throw new BusinessException(
                    "Não é possível movimentar o estoque de um filamento desativado.");
        }

        BigDecimal saldoAtual = filamento.getQuantidadeEstoqueG();
        BigDecimal novoSaldo = request.tipo() == StockMovementType.ENTRADA
                ? saldoAtual.add(request.quantidadeG())
                : saldoAtual.subtract(request.quantidadeG());

        if (novoSaldo.signum() < 0) {
            throw new BusinessException(
                    "Saldo insuficiente: o estoque atual é de %s g e a saída solicitada é de %s g."
                            .formatted(saldoAtual.stripTrailingZeros().toPlainString(),
                                    request.quantidadeG().stripTrailingZeros().toPlainString()));
        }

        filamento.setQuantidadeEstoqueG(novoSaldo);
        return filamentMapper.toResponse(filamento);
    }

    /** Soft delete: preserva o histórico de uso em pedidos/orçamentos. */
    @Transactional
    public void desativar(Long id) {
        Filament filamento = obterFilamento(id);
        filamento.setAtivo(false);
    }

    @Transactional
    public FilamentResponse reativar(Long id) {
        Filament filamento = obterFilamento(id);
        filamento.setAtivo(true);
        return filamentMapper.toResponse(filamento);
    }

    private Filament obterFilamento(Long id) {
        return filamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filamento", id));
    }
}
