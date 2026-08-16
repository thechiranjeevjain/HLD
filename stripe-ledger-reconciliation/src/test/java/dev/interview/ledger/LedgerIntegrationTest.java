package dev.interview.ledger;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class LedgerIntegrationTest {
  @Autowired LedgerService ledger; @Autowired ReconciliationService rec; @Autowired ReconciliationWorker worker; @Autowired JdbcTemplate db;@Autowired ExternalImportService imports;
  @BeforeEach void clean(){for(String t:java.util.List.of("quarantined_external_rows","external_imports","outbox_events","adjustment_requests","reconciliation_items","reconciliation_shards","reconciliation_runs","external_transactions","audit_events","ledger_entries","balance_projection","webhook_events","idempotency_requests","payments"))db.update("delete from "+t);}

  @Test void sameIdempotencyKeyReturnsSamePaymentAndOneJournal(){
    var req=new ApiModels.CreatePayment("cus_1","order_1",1000,"USD","USD",false);
    var first=ledger.createPayment("same-key",req); var retry=ledger.createPayment("same-key",req);
    assertThat(retry.get("paymentId")).isEqualTo(first.get("paymentId"));
    assertThat(db.queryForObject("select count(*) from payments",Integer.class)).isEqualTo(1);
    assertThat(db.queryForObject("select count(distinct journal_id) from ledger_entries",Integer.class)).isEqualTo(1);
    assertThat(db.queryForObject("select sum(amount) from ledger_entries",Long.class)).isZero();
  }

  @Test void reusedKeyWithDifferentPayloadIsRejected(){
    ledger.createPayment("key",new ApiModels.CreatePayment("cus","a",100,"USD","USD",false));
    assertThatThrownBy(()->ledger.createPayment("key",new ApiModels.CreatePayment("cus","b",200,"USD","USD",false)))
      .isInstanceOf(LedgerService.Conflict.class).hasMessageContaining("DIFFERENT_REQUEST");
  }

  @Test void duplicateAndLateWebhookAreSafe(){
    var p=ledger.createPayment("capture-fails",new ApiModels.CreatePayment("cus","order",750,"EUR","USD",true));
    var event=new ApiModels.Webhook("evt_1","payment_intent.succeeded",(String)p.get("paymentIntentId"),"ch_1",750,"EUR",null,null,null,null);
    assertThat(ledger.processWebhook(event).get("outcome")).isEqualTo("APPLIED");
    assertThat(ledger.processWebhook(event).get("outcome")).isEqualTo("DUPLICATE_IGNORED");
    assertThat(db.queryForObject("select count(*) from ledger_entries",Integer.class)).isEqualTo(2);
  }

  @Test void reconciliationIsDeterministicAndRunIsIdempotent(){
    var p=ledger.createPayment("p",new ApiModels.CreatePayment("cus","o",500,"USD","USD",false));
    rec.importExternal(new ApiModels.ImportExternal("txn",(String)p.get("paymentIntentId"),450,"USD","AVAILABLE"));
    String id=(String)rec.create(new ApiModels.CreateReconciliation("STRIPE",LocalDate.now().minusDays(1),LocalDate.now().plusDays(1))).get("reconciliationId");
    rec.run(id,"run-key");worker.processAvailable();var first=rec.details(id); var replay=rec.run(id,"run-key");
    assertThat(first.get("items").toString()).isEqualTo(replay.get("items").toString()).contains("AMOUNT_DIFFERENCE");
    assertThat(db.queryForObject("select count(*) from reconciliation_items where run_id=?",Integer.class,id)).isEqualTo(1);
    assertThat(db.queryForObject("select count(*) from reconciliation_shards where run_id=? and status='COMPLETED'",Integer.class,id)).isEqualTo(4);
  }

  @Test void malformedStatementRowsAreQuarantinedWithoutDroppingGoodRows(){
    var rows=java.util.List.<java.util.Map<String,Object>>of(java.util.Map.of("externalId","tx1","matchKey","pi1","amount",100,"currency","USD","status","AVAILABLE"),java.util.Map.of("externalId","tx2","amount","bad","currency","USD","status","AVAILABLE"));
    var result=imports.ingest(new ApiModels.ExternalFile("stripe.csv","v1",rows));
    assertThat(result.get("accepted")).isEqualTo(1);assertThat(result.get("quarantined")).isEqualTo(1);
  }

  @Test void largeAdjustmentRequiresApprovalAndThenPostsCompensatingJournal(){
    String run=(String)rec.create(new ApiModels.CreateReconciliation("STRIPE",LocalDate.now(),LocalDate.now())).get("reconciliationId");
    var req=new ApiModels.Adjustment(run,"manual settlement correction",java.util.List.of(new ApiModels.AdjustmentEntry("processor_cash","USD",15000,"tx"),new ApiModels.AdjustmentEntry("reconciliation_suspense","USD",-15000,"tx")));
    var pending=rec.requestAdjustment(req);assertThat(pending.get("status")).isEqualTo("PENDING_APPROVAL");
    var posted=rec.approveAdjustment((String)pending.get("adjustmentRequestId"),"senior-ops");assertThat(posted.get("status")).isEqualTo("POSTED");
    assertThat(db.queryForObject("select sum(amount) from ledger_entries where journal_id=?",Long.class,posted.get("journalId"))).isZero();
  }

  @Test void concurrentRegionalPathsProduceOnePayment(){
    var request=new ApiModels.CreatePayment("cus_regions","order_regions",999,"USD","USD",false);
    try(var executor=java.util.concurrent.Executors.newFixedThreadPool(8)){
      var futures=new java.util.ArrayList<java.util.concurrent.Future<java.util.Map<String,Object>>>();
      for(int i=0;i<16;i++)futures.add(executor.submit(()->ledger.createPayment("global-key",request)));
      var ids=new java.util.HashSet<String>();for(var future:futures)ids.add((String)future.get(10,java.util.concurrent.TimeUnit.SECONDS).get("paymentId"));
      assertThat(ids).hasSize(1);assertThat(db.queryForObject("select count(*) from payments where order_id='order_regions'",Integer.class)).isEqualTo(1);
    }catch(Exception e){throw new RuntimeException(e);}
  }

  @Test void fxSettlementCreatesBalancedLegsInEachCurrency(){
    var p=ledger.createPayment("fx",new ApiModels.CreatePayment("cus_fx","order_fx",10000,"EUR","USD",false));
    ledger.processWebhook(new ApiModels.Webhook("evt_fx","balance.available",(String)p.get("paymentIntentId"),"txn_fx",10000,"EUR",null,10850L,"USD","1.085"));
    var sums=db.queryForList("select currency,sum(amount) total from ledger_entries group by currency");
    assertThat(sums).allSatisfy(x->assertThat(((Number)x.get("TOTAL")).longValue()).isZero());
  }
}
