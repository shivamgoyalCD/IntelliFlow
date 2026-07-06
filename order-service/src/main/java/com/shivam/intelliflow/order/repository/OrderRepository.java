package com.shivam.intelliflow.order.repository;

import com.shivam.intelliflow.order.entity.Order;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
