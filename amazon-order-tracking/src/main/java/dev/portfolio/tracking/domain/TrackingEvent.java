package dev.portfolio.tracking.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(indexes=@Index(name="idx_event_shipment_time",columnList="shipmentId,eventTime"), uniqueConstraints={@UniqueConstraint(name="uq_idempotency",columnNames="idempotencyKey"),@UniqueConstraint(name="uq_payload_hash",columnNames={"shipmentId","rawPayloadHash"})})
public class TrackingEvent {
  @Id private String id;
  @Column(nullable=false) private String shipmentId;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private TrackingStatus eventType;
  @Column(nullable=false) private Instant eventTime;
  @Column(nullable=false) private Instant receivedTime;
  private String location;
  @Column(nullable=false) private String rawPayloadHash;
  @Column(nullable=false) private String idempotencyKey;
  protected TrackingEvent() {}
  public TrackingEvent(String id,String shipmentId,TrackingStatus eventType,Instant eventTime,Instant receivedTime,String location,String rawPayloadHash,String idempotencyKey){this.id=id;this.shipmentId=shipmentId;this.eventType=eventType;this.eventTime=eventTime;this.receivedTime=receivedTime;this.location=location;this.rawPayloadHash=rawPayloadHash;this.idempotencyKey=idempotencyKey;}
  public String getId(){return id;} public String getShipmentId(){return shipmentId;} public TrackingStatus getEventType(){return eventType;} public Instant getEventTime(){return eventTime;} public Instant getReceivedTime(){return receivedTime;} public String getLocation(){return location;} public String getRawPayloadHash(){return rawPayloadHash;} public String getIdempotencyKey(){return idempotencyKey;}
}
