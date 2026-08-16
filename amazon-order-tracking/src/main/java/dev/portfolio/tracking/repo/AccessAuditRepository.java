package dev.portfolio.tracking.repo;
import dev.portfolio.tracking.domain.AccessAudit;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccessAuditRepository extends JpaRepository<AccessAudit,Long>{}
