package com.print3dmanager.erp.filament.repository;

import com.print3dmanager.erp.filament.model.Filament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FilamentRepository
        extends JpaRepository<Filament, Long>, JpaSpecificationExecutor<Filament> {
}
