package com.interview.fraud.transaction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="transactions")
public class TransactionEntity {
 @Id public UUID id; @Column(name="external_id",unique=true) public String externalId; @Column(name="customer_id") public String customerId;
 @Column(name="merchant_id") public String merchantId; @Column(name="device_id") public String deviceId; public BigDecimal amount; public String currency; public String country;
 @Column(name="occurred_at") public Instant occurredAt; public String status; @Version public long version;
 protected TransactionEntity() {}
 public TransactionEntity(TransactionRequest r){id=UUID.randomUUID();externalId=r.transactionId();customerId=r.customerId();merchantId=r.merchantId();deviceId=r.deviceId();amount=r.amount();currency=r.currency();country=r.country();occurredAt=r.occurredAt()==null?Instant.now():r.occurredAt();status="RECEIVED";}
}
