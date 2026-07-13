package com.print3dmanager.erp.printer.repository;

import com.print3dmanager.erp.printer.model.Printer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PrinterRepository
        extends JpaRepository<Printer, Long>, JpaSpecificationExecutor<Printer> {
}
