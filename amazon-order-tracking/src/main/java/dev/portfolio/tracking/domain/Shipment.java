package dev.portfolio.tracking.domain;

import jakarta.persistence.*;

@Entity @Table(indexes={@Index(name="idx_shipment_order",columnList="orderId"),@Index(name="idx_carrier_tracking",columnList="carrier,trackingNumber",unique=true)})
public class Shipment {
  @Id private String id;
  @Column(nullable=false) private String orderId;
  @Column(nullable=false) private String carrier;
  @Column(nullable=false) private String trackingNumber;
  @Column(nullable=false) private String addressHash;
  protected Shipment() {}
  public Shipment(String id,String orderId,String carrier,String trackingNumber,String addressHash){this.id=id;this.orderId=orderId;this.carrier=carrier;this.trackingNumber=trackingNumber;this.addressHash=addressHash;}
  public String getId(){return id;} public String getOrderId(){return orderId;} public String getCarrier(){return carrier;} public String getTrackingNumber(){return trackingNumber;} public String getAddressHash(){return addressHash;}
}
