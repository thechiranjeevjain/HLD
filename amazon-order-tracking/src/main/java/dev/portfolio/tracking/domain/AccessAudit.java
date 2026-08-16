package dev.portfolio.tracking.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(indexes=@Index(name="idx_audit_order",columnList="orderId,accessedAt"))
public class AccessAudit {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false) private String orderId;
  @Column(nullable=false) private String actorId;
  @Column(nullable=false) private String actorRole;
  @Column(nullable=false) private Instant accessedAt;
  protected AccessAudit() {}
  public AccessAudit(String orderId,String actorId,String actorRole,Instant accessedAt){this.orderId=orderId;this.actorId=actorId;this.actorRole=actorRole;this.accessedAt=accessedAt;}
}
