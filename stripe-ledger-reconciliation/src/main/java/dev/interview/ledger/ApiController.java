package dev.interview.ledger;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final LedgerService ledger; private final ReconciliationService reconciliation; private final DemoService demo;private final ExternalImportService imports;private final ScaleDatasetService scale;
  ApiController(LedgerService ledger,ReconciliationService reconciliation,DemoService demo,ExternalImportService imports,ScaleDatasetService scale){this.ledger=ledger;this.reconciliation=reconciliation;this.demo=demo;this.imports=imports;this.scale=scale;}

  @PostMapping("/payments") Map<String,Object> payment(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ApiModels.CreatePayment body){return ledger.createPayment(key,body);}
  @PostMapping("/webhooks/stripe") Map<String,Object> webhook(@Valid @RequestBody ApiModels.Webhook body){return ledger.processWebhook(body);}
  @GetMapping("/payments") List<Map<String,Object>> payments(){return ledger.payments();}
  @GetMapping("/ledger") List<Map<String,Object>> entries(){return ledger.ledger();}
  @GetMapping("/balances") List<Map<String,Object>> balances(){return ledger.balances();}
  @GetMapping("/support/payments/{id}") Map<String,Object> support(@PathVariable String id){return ledger.support(id);}
  @PostMapping("/external-transactions") ResponseEntity<Void> external(@Valid @RequestBody ApiModels.ImportExternal body){reconciliation.importExternal(body);return ResponseEntity.accepted().build();}
  @PostMapping("/external-imports") Map<String,Object> importFile(@Valid @RequestBody ApiModels.ExternalFile body){return imports.ingest(body);}
  @GetMapping("/external-imports") List<Map<String,Object>> imports(){return imports.imports();}
  @GetMapping("/external-imports/quarantine") List<Map<String,Object>> quarantine(){return imports.quarantine();}
  @PostMapping("/reconciliations") ResponseEntity<Map<String,Object>> create(@Valid @RequestBody ApiModels.CreateReconciliation body){return ResponseEntity.status(201).body(reconciliation.create(body));}
  @PostMapping("/reconciliations/{id}/run") Map<String,Object> run(@PathVariable String id,@RequestHeader("Idempotency-Key") String key){return reconciliation.run(id,key);}
  @GetMapping("/reconciliations/{id}") Map<String,Object> get(@PathVariable String id){return reconciliation.details(id);}
  @GetMapping("/reconciliations") List<Map<String,Object>> all(){return reconciliation.all();}
  @PostMapping("/adjustments") Map<String,Object> adjustment(@Valid @RequestBody ApiModels.Adjustment body){return reconciliation.requestAdjustment(body);}
  @PostMapping("/adjustments/{id}/approve") Map<String,Object> approve(@PathVariable String id,@RequestHeader("X-Approver") String approver){return reconciliation.approveAdjustment(id,approver);}
  @GetMapping("/outbox") List<Map<String,Object>> outbox(){return reconciliation.outbox();}
  @PostMapping("/demo/reset-and-seed") Map<String,Object> seed(){return demo.resetAndSeed();}
  @PostMapping("/demo/generate-scale") Map<String,Object> scale(@Valid @RequestBody ApiModels.ScaleRequest body){return scale.generate(body);}

  @ExceptionHandler(LedgerService.Conflict.class) ResponseEntity<Map<String,Object>> conflict(Exception e){return ResponseEntity.status(409).body(Map.of("error",e.getMessage()));}
  @ExceptionHandler({IllegalArgumentException.class,org.springframework.web.bind.MethodArgumentNotValidException.class}) ResponseEntity<Map<String,Object>> bad(Exception e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
}
