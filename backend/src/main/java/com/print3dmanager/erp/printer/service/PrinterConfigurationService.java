package com.print3dmanager.erp.printer.service;

import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.printer.dto.PrinterConfigRequest;
import com.print3dmanager.erp.printer.dto.PrinterConfigResponse;
import com.print3dmanager.erp.printer.mapper.PrinterMapper;
import com.print3dmanager.erp.printer.model.Printer;
import com.print3dmanager.erp.printer.model.PrinterConfiguration;
import com.print3dmanager.erp.printer.repository.PrinterConfigurationRepository;
import com.print3dmanager.erp.printer.repository.PrinterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configurações de custo para orçamentos: uma global (obrigatória para
 * orçar) e, opcionalmente, uma por impressora que a sobrepõe.
 */
@Service
@RequiredArgsConstructor
public class PrinterConfigurationService {

    public static final String ORIGEM_GLOBAL = "GLOBAL";
    public static final String ORIGEM_PROPRIA = "PROPRIA";

    private final PrinterConfigurationRepository configRepository;
    private final PrinterRepository printerRepository;
    private final PrinterMapper printerMapper;

    @Transactional(readOnly = true)
    public PrinterConfigResponse buscarGlobal() {
        return printerMapper.toConfigResponse(obterGlobal(), ORIGEM_GLOBAL);
    }

    /** Cria ou atualiza a configuração global (upsert). */
    @Transactional
    public PrinterConfigResponse salvarGlobal(PrinterConfigRequest request) {
        PrinterConfiguration config = configRepository.findByImpressoraIsNull()
                .orElseGet(PrinterConfiguration::new);
        printerMapper.atualizarConfig(config, request);
        return printerMapper.toConfigResponse(configRepository.save(config), ORIGEM_GLOBAL);
    }

    /**
     * Configuração efetiva da impressora: a própria, se existir;
     * caso contrário, a global.
     */
    @Transactional(readOnly = true)
    public PrinterConfigResponse buscarEfetiva(Long impressoraId) {
        obterImpressora(impressoraId);
        return configRepository.findByImpressoraId(impressoraId)
                .map(config -> printerMapper.toConfigResponse(config, ORIGEM_PROPRIA))
                .orElseGet(() -> printerMapper.toConfigResponse(obterGlobal(), ORIGEM_GLOBAL));
    }

    /** Cria ou atualiza a configuração própria da impressora (upsert). */
    @Transactional
    public PrinterConfigResponse salvarDaImpressora(Long impressoraId,
                                                    PrinterConfigRequest request) {
        Printer impressora = obterImpressora(impressoraId);
        PrinterConfiguration config = configRepository.findByImpressoraId(impressoraId)
                .orElseGet(() -> {
                    PrinterConfiguration nova = new PrinterConfiguration();
                    nova.setImpressora(impressora);
                    return nova;
                });
        printerMapper.atualizarConfig(config, request);
        return printerMapper.toConfigResponse(configRepository.save(config), ORIGEM_PROPRIA);
    }

    /** Remove a configuração própria — a impressora volta a usar a global. */
    @Transactional
    public void removerDaImpressora(Long impressoraId) {
        obterImpressora(impressoraId);
        PrinterConfiguration config = configRepository.findByImpressoraId(impressoraId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Esta impressora não possui configuração própria de custos."));
        configRepository.delete(config);
    }

    private PrinterConfiguration obterGlobal() {
        return configRepository.findByImpressoraIsNull()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma configuração global de custos cadastrada. "
                                + "Configure-a em PUT /printers/config."));
    }

    private Printer obterImpressora(Long id) {
        return printerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impressora", id));
    }
}
