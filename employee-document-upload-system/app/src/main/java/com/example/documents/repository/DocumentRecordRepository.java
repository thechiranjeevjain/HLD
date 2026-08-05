package com.example.documents.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.documents.domain.DocumentRecord;

public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, UUID> {

    List<DocumentRecord> findAllByOrderByCreatedAtDesc();

    List<DocumentRecord> findByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId);
}
