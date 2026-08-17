package com.example.capstone.ecommerce.payment;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentRecord, UUID> {
}
