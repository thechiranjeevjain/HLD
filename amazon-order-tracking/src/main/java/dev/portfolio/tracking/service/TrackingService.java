package dev.portfolio.tracking.service;

import com.github.benmanes.caffeine.cache.*;
import dev.portfolio.tracking.api.ApiModels.*;
import dev.portfolio.tracking.domain.*;
import dev.portfolio.tracking.repo.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class TrackingService {
  private final OrderRepository orders; private final ShipmentRepository shipments; private final TrackingEventRepository events;
  private final ShipmentStateRepository states; private final AccessAuditRepository audits;
  private final Cache<String,TrackingResponse> cache= Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(10)).maximumSize(10_000).build();
  private final List<DeadLetter> deadLetters=new CopyOnWriteArrayList<>();
  public TrackingService(OrderRepository orders,ShipmentRepository shipments,TrackingEventRepository events,ShipmentStateRepository states,AccessAuditRepository audits){this.orders=orders;this.shipments=shipments;this.events=events;this.states=states;this.audits=audits;}

  @Transactional
  public IngestResponse ingest(CarrierEventRequest r){
    Shipment shipment=shipments.findByCarrierIgnoreCaseAndTrackingNumber(r.carrier(),r.trackingNumber()).orElseThrow(()->new EntityNotFoundException("Unknown carrier tracking number"));
    String hash=sha256(r.rawPayload());
    if(events.existsByIdempotencyKey(r.idempotencyKey())||events.existsByShipmentIdAndRawPayloadHash(shipment.getId(),hash)) return new IngestResponse("DUPLICATE",null,shipment.getId(),states.findById(shipment.getId()).map(ShipmentState::getVersion).orElse(0L));
    String eventId=UUID.randomUUID().toString();
    TrackingEvent event=new TrackingEvent(eventId,shipment.getId(),r.eventType(),r.eventTime(),Instant.now(),r.location(),hash,r.idempotencyKey());
    try { events.saveAndFlush(event); } catch(DataIntegrityViolationException duplicate){ return new IngestResponse("DUPLICATE",null,shipment.getId(),states.findById(shipment.getId()).map(ShipmentState::getVersion).orElse(0L)); }
    ShipmentState state=rebuildState(shipment.getId());
    cache.invalidate(shipment.getOrderId());
    return new IngestResponse("ACCEPTED",eventId,shipment.getId(),state.getVersion());
  }

  /** Replays immutable events in event-time order: the same function is used for normal processing and backfills. */
  ShipmentState rebuildState(String shipmentId){
    List<TrackingEvent> history=events.findByShipmentIdOrderByEventTimeAscReceivedTimeAsc(shipmentId);
    TrackingEvent winner=history.stream().max(Comparator.comparing(TrackingEvent::getEventTime).thenComparing(e->e.getEventType().rank()).thenComparing(TrackingEvent::getReceivedTime)).orElseThrow();
    ShipmentState state=states.findById(shipmentId).orElse(new ShipmentState(shipmentId,winner.getEventType(),winner.getEventTime()));
    state.update(winner.getEventType(),winner.getEventTime());
    return states.saveAndFlush(state);
  }

  @Transactional
  public TrackingResponse tracking(String orderId,String actorId,String role){ authorize(orderId,actorId,role); audits.save(new AccessAudit(orderId,actorId,role,Instant.now())); return cache.get(orderId,this::buildTracking); }

  @Transactional
  public ShipmentsResponse shipmentList(String orderId,String actorId,String role){
    authorize(orderId,actorId,role); audits.save(new AccessAudit(orderId,actorId,role,Instant.now()));
    List<ShipmentSummary> result=shipments.findByOrderIdOrderById(orderId).stream().map(s->{ShipmentState st=states.findById(s.getId()).orElseThrow();return new ShipmentSummary(s.getId(),s.getCarrier(),s.getTrackingNumber(),st.getStatus(),st.getStatusTime());}).toList();
    return new ShipmentsResponse(orderId,result);
  }

  private TrackingResponse buildTracking(String orderId){
    List<ShipmentView> views=shipments.findByOrderIdOrderById(orderId).stream().map(s->{
      ShipmentState st=states.findById(s.getId()).orElseThrow();
      List<TimelineItem> timeline=events.findByShipmentIdOrderByEventTimeAscReceivedTimeAsc(s.getId()).stream().map(e->new TimelineItem(e.getId(),e.getShipmentId(),e.getEventType(),e.getEventTime(),e.getReceivedTime(),e.getLocation())).toList();
      return new ShipmentView(s.getId(),s.getCarrier(),s.getTrackingNumber(),st.getStatus(),st.getStatusTime(),st.getVersion(),timeline);
    }).toList();
    List<TimelineItem> merged=views.stream().flatMap(v->v.timeline().stream()).sorted(Comparator.comparing(TimelineItem::eventTime)).toList();
    ShipmentView latest=views.stream().max(Comparator.comparing(ShipmentView::lastUpdateTs)).orElseThrow();
    TrackingStatus aggregate=views.stream().allMatch(v->v.currentStatus()==TrackingStatus.DELIVERED)?TrackingStatus.DELIVERED:latest.currentStatus();
    return new TrackingResponse(orderId,aggregate,latest.lastUpdateTs(),views,merged);
  }

  private void authorize(String orderId,String actorId,String role){
    OrderEntity order=orders.findById(orderId).orElseThrow(()->new EntityNotFoundException("Order not found"));
    if(!"SUPPORT".equalsIgnoreCase(role)&&!order.getUserId().equals(actorId)) throw new SecurityException("Only the buyer or audited support may view this order");
  }
  public List<DeadLetter> deadLetters(){return List.copyOf(deadLetters);}
  public void deadLetter(String reason,String carrier,String tracking,String payload){deadLetters.add(new DeadLetter(UUID.randomUUID().toString(),Instant.now(),reason,carrier,tracking,payload));}
  private static String sha256(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(b);}catch(Exception e){throw new IllegalStateException(e);}}
}
