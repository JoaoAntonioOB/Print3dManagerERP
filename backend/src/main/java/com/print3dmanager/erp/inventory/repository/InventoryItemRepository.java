package com.print3dmanager.erp.inventory.repository;

import com.print3dmanager.erp.inventory.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InventoryItemRepository
        extends JpaRepository<InventoryItem, Long>, JpaSpecificationExecutor<InventoryItem> {
}
