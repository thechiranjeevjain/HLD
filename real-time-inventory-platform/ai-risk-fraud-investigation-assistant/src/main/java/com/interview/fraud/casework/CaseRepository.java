package com.interview.fraud.casework; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface CaseRepository extends JpaRepository<CaseEntity,UUID>{Optional<CaseEntity> findByCaseNumber(String number); List<CaseEntity> findAllByOrderByCreatedAtDesc();}
