package dev.interview.ledger;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
public class DemoService {
  private final JdbcTemplate db; private final LedgerService ledger; private final ReconciliationService rec;private final ReconciliationWorker worker;
  DemoService(JdbcTemplate db,LedgerService ledger,ReconciliationService rec,ReconciliationWorker worker){this.db=db;this.ledger=ledger;this.rec=rec;this.worker=worker;}
  public Map<String,Object> resetAndSeed(){
    for(String t:List.of("quarantined_external_rows","external_imports","outbox_events","adjustment_requests","reconciliation_items","reconciliation_shards","reconciliation_runs","external_transactions","audit_events","ledger_entries","balance_projection","webhook_events","idempotency_requests","payments")) db.update("delete from "+t);
    var p1=ledger.createPayment("demo-order-1001",new ApiModels.CreatePayment("cus_ada","order_1001",10000,"USD","USD",false));
    var p2=ledger.createPayment("demo-order-1002",new ApiModels.CreatePayment("cus_grace","order_1002",7500,"EUR","USD",true));
    String pi2=(String)p2.get("paymentIntentId"), pay2=(String)p2.get("paymentId");
    ledger.processWebhook(new ApiModels.Webhook("evt_late_capture","payment_intent.succeeded",pi2,"ch_late",7500,"EUR","late success after timeout",null,null,null));
    ledger.processWebhook(new ApiModels.Webhook("evt_refund_1","charge.refunded",(String)p1.get("paymentIntentId"),"re_partial_1",2500,"USD","partial refund",null,null,null));
    rec.importExternal(new ApiModels.ImportExternal("txn_match_1",(String)p1.get("paymentIntentId"),10000,"USD","AVAILABLE"));
    rec.importExternal(new ApiModels.ImportExternal("txn_fx_1",pi2,7425,"EUR","AVAILABLE"));
    var created=rec.create(new ApiModels.CreateReconciliation("STRIPE",LocalDate.now().minusDays(1),LocalDate.now().plusDays(1)));
    String recId=(String)created.get("reconciliationId"); rec.run(recId,"demo-run-once");worker.processAvailable();
    return Map.of("status","SEEDED","payments",List.of(p1,p2),"reconciliationId",recId,"supportExample","/api/support/payments/"+pay2);
  }
}
