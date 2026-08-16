package dev.portfolio.tracking.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="customer_orders", indexes=@Index(name="idx_order_user", columnList="userId"))
public class OrderEntity {
  @Id private String id;
  @Column(nullable=false) private String userId;
  @Column(nullable=false) private Instant createdAt;
  protected OrderEntity() {}
  public OrderEntity(String id, String userId, Instant createdAt) { this.id=id; this.userId=userId; this.createdAt=createdAt; }
  public String getId(){return id;} public String getUserId(){return userId;} public Instant getCreatedAt(){return createdAt;}
}
