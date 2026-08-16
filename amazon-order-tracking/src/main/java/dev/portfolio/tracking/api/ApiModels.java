package dev.portfolio.tracking.api;

import dev.portfolio.tracking.domain.TrackingStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class ApiModels {
  private ApiModels() {}
  public record CarrierEventRequest(
      @NotBlank String idempotencyKey, @NotBlank String carrier, @NotBlank String trackingNumber,
      @NotNull TrackingStatus eventType, @NotNull @PastOrPresent Instant eventTime,
      @Size(max=120) String location, @NotBlank String rawPayload) {}
  public record IngestResponse(String result,String eventId,String shipmentId,long stateVersion) {}
  public record TimelineItem(String eventId,String shipmentId,TrackingStatus status,Instant eventTime,Instant receivedTime,String location) {}
  public record ShipmentView(String shipmentId,String carrier,String trackingNumber,TrackingStatus currentStatus,Instant lastUpdateTs,long version,List<TimelineItem> timeline) {}
  public record TrackingResponse(String orderId,TrackingStatus currentStatus,Instant lastUpdateTs,List<ShipmentView> shipments,List<TimelineItem> timeline) {}
  public record ShipmentSummary(String shipmentId,String carrier,String trackingNumber,TrackingStatus status,Instant lastUpdateTs) {}
  public record ShipmentsResponse(String orderId,List<ShipmentSummary> shipments) {}
  public record ErrorResponse(String code,String message,Instant timestamp) {}
  public record DeadLetter(String id,Instant receivedAt,String reason,String carrier,String trackingNumber,String payload) {}
}
