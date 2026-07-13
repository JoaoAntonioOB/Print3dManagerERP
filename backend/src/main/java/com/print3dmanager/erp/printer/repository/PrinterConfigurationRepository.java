package com.print3dmanager.erp.printer.repository;

import com.print3dmanager.erp.printer.model.PrinterConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrinterConfigurationRepository
        extends JpaRepository<PrinterConfiguration, Long> {

    /** Configuração global de custos (sem impressora vinculada). */
    Optional<PrinterConfiguration> findByImpressoraIsNull();

    Optional<PrinterConfiguration> findByImpressoraId(Long impressoraId);
}
