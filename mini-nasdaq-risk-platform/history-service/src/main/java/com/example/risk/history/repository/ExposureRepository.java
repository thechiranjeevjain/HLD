package com.example.risk.history.repository;

import com.example.risk.history.domain.ExposureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface ExposureRepository extends JpaRepository<ExposureEntity, UUID> {
    Optional<ExposureEntity> findByClientIdAndSymbol(String clientId, String symbol);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExposureEntity> findWithLockByClientIdAndSymbol(String clientId, String symbol);
}
