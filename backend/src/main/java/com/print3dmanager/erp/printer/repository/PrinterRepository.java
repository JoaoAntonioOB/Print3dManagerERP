package com.print3dmanager.erp.printer.repository;

import com.print3dmanager.erp.printer.model.Printer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PrinterRepository
        extends JpaRepository<Printer, Long>, JpaSpecificationExecutor<Printer> {

    /**
     * Carrega a impressora com lock pessimista de escrita, para que a
     * checagem de disponibilidade e a ocupação (status IMPRIMINDO) sejam
     * atômicas em relação a outra chamada concorrente de início de job na
     * mesma máquina.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Printer p where p.id = :id")
    Optional<Printer> findByIdForUpdate(@Param("id") Long id);
}
