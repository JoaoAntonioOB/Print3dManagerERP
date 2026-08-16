package com.print3dmanager.erp.filament.repository;

import com.print3dmanager.erp.filament.model.Filament;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FilamentRepository
        extends JpaRepository<Filament, Long>, JpaSpecificationExecutor<Filament> {

    /**
     * Carrega o filamento com lock pessimista de escrita, para que leitura e
     * gravação de {@code quantidadeEstoqueG} fiquem protegidas contra
     * concorrência (duas movimentações/jobs abatendo o mesmo saldo ao mesmo
     * tempo).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Filament f where f.id = :id")
    Optional<Filament> findByIdForUpdate(@Param("id") Long id);
}
