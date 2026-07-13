package com.print3dmanager.erp.printer.service;

import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.printer.dto.PrinterCreateRequest;
import com.print3dmanager.erp.printer.dto.PrinterResponse;
import com.print3dmanager.erp.printer.dto.PrinterUpdateRequest;
import com.print3dmanager.erp.printer.mapper.PrinterMapper;
import com.print3dmanager.erp.printer.model.Printer;
import com.print3dmanager.erp.printer.model.PrinterStatus;
import com.print3dmanager.erp.printer.repository.PrinterRepository;
import com.print3dmanager.erp.printer.repository.PrinterSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio de impressoras: CRUD com soft delete e controle
 * da situação operacional (status).
 */
@Service
@RequiredArgsConstructor
public class PrinterService {

    private final PrinterRepository printerRepository;
    private final PrinterMapper printerMapper;

    @Transactional(readOnly = true)
    public PageResponse<PrinterResponse> listar(String busca, PrinterStatus status, Boolean ativo,
                                                Pageable pageable) {
        return PageResponse.de(
                printerRepository.findAll(
                                PrinterSpecifications.comFiltros(busca, status, ativo), pageable)
                        .map(printerMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PrinterResponse buscarPorId(Long id) {
        return printerMapper.toResponse(obterImpressora(id));
    }

    @Transactional
    public PrinterResponse criar(PrinterCreateRequest request) {
        Printer impressora = printerMapper.toEntity(request);
        return printerMapper.toResponse(printerRepository.save(impressora));
    }

    @Transactional
    public PrinterResponse atualizar(Long id, PrinterUpdateRequest request) {
        Printer impressora = obterImpressora(id);
        printerMapper.atualizar(impressora, request);
        return printerMapper.toResponse(impressora);
    }

    @Transactional
    public PrinterResponse alterarStatus(Long id, PrinterStatus novoStatus) {
        Printer impressora = obterImpressora(id);
        if (!impressora.isAtivo()) {
            throw new BusinessException(
                    "Não é possível alterar o status de uma impressora desativada.");
        }
        impressora.setStatus(novoStatus);
        return printerMapper.toResponse(impressora);
    }

    /** Soft delete: desativa e marca como INATIVA (some das listagens ativas). */
    @Transactional
    public void desativar(Long id) {
        Printer impressora = obterImpressora(id);
        impressora.setAtivo(false);
        impressora.setStatus(PrinterStatus.INATIVA);
    }

    @Transactional
    public PrinterResponse reativar(Long id) {
        Printer impressora = obterImpressora(id);
        impressora.setAtivo(true);
        impressora.setStatus(PrinterStatus.DISPONIVEL);
        return printerMapper.toResponse(impressora);
    }

    private Printer obterImpressora(Long id) {
        return printerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impressora", id));
    }
}
