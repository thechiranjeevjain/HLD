package dev.interview.orders.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "customer_orders")
public class CustomerOrder {
    @Id private UUID id;
    @Column(nullable=false) private String customerId;
    @Column(nullable=false) private String sku;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false, precision=19, scale=2) private BigDecimal unitPrice;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private OrderStatus status;
    @Column(nullable=false, unique=true) private String idempotencyKey;
    @Column(nullable=false) private Instant createdAt;
    @Version private long version;
    protected CustomerOrder() {}
    public CustomerOrder(UUID id,String customerId,String sku,int quantity,BigDecimal unitPrice,String key){this.id=id;this.customerId=customerId;this.sku=sku;this.quantity=quantity;this.unitPrice=unitPrice;this.idempotencyKey=key;this.status=OrderStatus.CREATED;this.createdAt=Instant.now();}
    public UUID getId(){return id;} public String getCustomerId(){return customerId;} public String getSku(){return sku;} public int getQuantity(){return quantity;} public BigDecimal getUnitPrice(){return unitPrice;} public OrderStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public long getVersion(){return version;}
    public void transitionTo(OrderStatus next){if(status==OrderStatus.FULFILLED||status==OrderStatus.REJECTED)throw new IllegalStateException("terminal order cannot transition"); status=next;}
}
