package dev.portfolio.tracking.config;

import dev.portfolio.tracking.domain.*;
import dev.portfolio.tracking.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Instant;

@Configuration
public class DemoData {
  @Bean CommandLineRunner seed(OrderRepository orders,ShipmentRepository shipments,TrackingEventRepository events,ShipmentStateRepository states){return args->{
    Instant now=Instant.now(); orders.save(new OrderEntity("ORD-1001","user-123",now.minusSeconds(172800)));
    shipments.save(new Shipment("SHP-1","ORD-1001","UPS","1Z-DEMO-001","sha256:demo-address"));
    shipments.save(new Shipment("SHP-2","ORD-1001","FEDEX","FX-DEMO-002","sha256:demo-address"));
    events.save(new TrackingEvent("EV-1","SHP-1",TrackingStatus.ORDERED,now.minusSeconds(172800),now.minusSeconds(172790),"Seattle, WA","seed-1","seed-1"));
    events.save(new TrackingEvent("EV-2","SHP-1",TrackingStatus.PACKED,now.minusSeconds(86400),now.minusSeconds(86390),"Kent, WA","seed-2","seed-2"));
    events.save(new TrackingEvent("EV-3","SHP-1",TrackingStatus.SHIPPED,now.minusSeconds(3600),now.minusSeconds(3550),"Portland, OR","seed-3","seed-3"));
    events.save(new TrackingEvent("EV-4","SHP-2",TrackingStatus.ORDERED,now.minusSeconds(172800),now.minusSeconds(172790),"Seattle, WA","seed-4","seed-4"));
    events.save(new TrackingEvent("EV-5","SHP-2",TrackingStatus.PACKED,now.minusSeconds(82000),now.minusSeconds(81950),"Kent, WA","seed-5","seed-5"));
    states.save(new ShipmentState("SHP-1",TrackingStatus.SHIPPED,now.minusSeconds(3600)));
    states.save(new ShipmentState("SHP-2",TrackingStatus.PACKED,now.minusSeconds(82000)));
  };}
}
