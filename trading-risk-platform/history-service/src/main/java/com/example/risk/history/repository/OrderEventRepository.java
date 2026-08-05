package com.example.risk.history.repository;

import com.example.risk.history.domain.OrderEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderEventRepository extends JpaRepository<OrderEventEntity, UUID> {
}
