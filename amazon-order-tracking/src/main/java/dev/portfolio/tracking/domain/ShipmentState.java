package dev.portfolio.tracking.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class ShipmentState {
  @Id private String shipmentId;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private TrackingStatus status;
  @Column(nullable=false) private Instant statusTime;
  @Version private long version;
  protected ShipmentState() {}
  public ShipmentState(String shipmentId,TrackingStatus status,Instant statusTime){this.shipmentId=shipmentId;this.status=status;this.statusTime=statusTime;}
  public void update(TrackingStatus status,Instant statusTime){this.status=status;this.statusTime=statusTime;}
  public String getShipmentId(){return shipmentId;} public TrackingStatus getStatus(){return status;} public Instant getStatusTime(){return statusTime;} public long getVersion(){return version;}
}
