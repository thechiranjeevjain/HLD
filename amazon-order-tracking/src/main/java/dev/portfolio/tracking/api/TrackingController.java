package dev.portfolio.tracking.api;

import dev.portfolio.tracking.api.ApiModels.*;
import dev.portfolio.tracking.service.TrackingService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class TrackingController {
  private final TrackingService service;
  public TrackingController(TrackingService service){this.service=service;}
  @GetMapping("/orders/{orderId}/tracking")
  public TrackingResponse tracking(@PathVariable String orderId,@RequestHeader("X-Actor-Id") String actor,@RequestHeader(value="X-Actor-Role",defaultValue="CUSTOMER") String role){return service.tracking(orderId,actor,role);}
  @GetMapping("/orders/{orderId}/shipments")
  public ShipmentsResponse shipments(@PathVariable String orderId,@RequestHeader("X-Actor-Id") String actor,@RequestHeader(value="X-Actor-Role",defaultValue="CUSTOMER") String role){return service.shipmentList(orderId,actor,role);}
  @PostMapping("/carrier/events")
  public ResponseEntity<IngestResponse> ingest(@Valid @RequestBody CarrierEventRequest request){IngestResponse result=service.ingest(request);return ResponseEntity.status(result.result().equals("ACCEPTED")?HttpStatus.ACCEPTED:HttpStatus.OK).body(result);}
  @GetMapping("/support/dead-letters") public List<DeadLetter> deadLetters(@RequestHeader("X-Actor-Role") String role){if(!"SUPPORT".equalsIgnoreCase(role))throw new SecurityException("Support role required");return service.deadLetters();}
}
