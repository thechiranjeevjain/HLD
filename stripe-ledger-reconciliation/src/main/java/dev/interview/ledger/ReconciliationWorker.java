package dev.interview.ledger;
import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Component;
@Component public class ReconciliationWorker{private final ReconciliationService service;ReconciliationWorker(ReconciliationService s){service=s;}@Scheduled(fixedDelayString="${app.reconciliation.poll-ms:500}")public void poll(){processAvailable();}public void processAvailable(){for(var r:service.readyShards())service.processShard((String)r.get("RUN_ID"),((Number)r.get("SHARD_ID")).intValue());}}
