package com.print3dmanager.erp.order.repository;

import com.print3dmanager.erp.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
