package dev.interview.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;

@Service
public class LedgerService {
  private static final Logger log=LoggerFactory.getLogger(LedgerService.class);
  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final MeterRegistry meters;
  LedgerService(JdbcTemplate db, ObjectMapper json, MeterRegistry meters) { this.db=db; this.json=json; this.meters=meters; }

  @Transactional
  public Map<String,Object> createPayment(String key, ApiModels.CreatePayment req) {
    String requestHash=Hashing.sha256(req.toString());
    var old=db.query("select request_hash,response_json from idempotency_requests where idempotency_key=?", rs ->
      rs.next()?Map.of("hash",rs.getString(1),"body",rs.getString(2)):null,key);
    if(old!=null) {
      if(!old.get("hash").equals(requestHash)) throw new Conflict("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST");
      try { return json.readValue((String)old.get("body"),Map.class); } catch(Exception e){ throw new IllegalStateException(e); }
    }
    String paymentId="pay_"+UUID.randomUUID().toString().substring(0,12);
    String pi="pi_demo_"+UUID.randomUUID().toString().substring(0,10);
    String charge="ch_demo_"+UUID.randomUUID().toString().substring(0,10);
    try { db.update("insert into idempotency_requests values(?,?,?,?,?)",key,requestHash,paymentId,"PENDING",OffsetDateTime.now()); }
    catch(DuplicateKeyException race) {
      var winner=db.queryForMap("select request_hash,response_json from idempotency_requests where idempotency_key=?",key);
      if(!requestHash.equals(winner.get("REQUEST_HASH"))) throw new Conflict("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST");
      try { return json.readValue((String)winner.get("RESPONSE_JSON"),Map.class); } catch(Exception e){ throw new IllegalStateException(e); }
    }
    String status=req.simulateCaptureFailure()?"CAPTURE_FAILED":"CAPTURED";
    int rank=req.simulateCaptureFailure()?20:30;
    db.update("insert into payments values(?,?,?,?,?,?,?,?,?,?,?,?)", paymentId,req.customerId(),req.orderId(),req.amount(),
      req.currency(),req.settlementCurrency(),status,rank,pi,charge,req.simulateCaptureFailure()?"PROCESSOR_TIMEOUT_AFTER_AUTH":null,OffsetDateTime.now());
    if(!req.simulateCaptureFailure()) {
      journal("CAPTURE",paymentId,pi,req.currency(),req.amount(),
        new Line("stripe_receivable",req.amount()),new Line("customer_funds",-req.amount()));
    }
    Map<String,Object> response=new LinkedHashMap<>();
    response.put("paymentId",paymentId); response.put("paymentIntentId",pi); response.put("chargeId",charge);
    response.put("status",status); response.put("amount",req.amount()); response.put("currency",req.currency()); response.put("idempotencyKey",key);
    String responseJson;
    try { responseJson=json.writeValueAsString(response); } catch(Exception e) { throw new IllegalStateException("cannot serialize idempotent response",e); }
    db.update("update idempotency_requests set response_json=? where idempotency_key=?",responseJson,key);
    audit("PAYMENT_REQUEST",paymentId,response.toString(),key);
    meters.counter("payments.created","status",status).increment();
    log.info("event=payment_created payment_id={} order_id={} idempotency_key={} status={}",paymentId,req.orderId(),key,status);
    return response;
  }

  @Transactional
  public Map<String,Object> processWebhook(ApiModels.Webhook event) {
    String payloadHash=Hashing.sha256(event.toString());
    try { db.update("insert into webhook_events(event_id,event_type,object_id,payload_hash,status,received_at,stripe_object_id,amount,currency,settlement_amount,settlement_currency,fx_rate,reason,attempt,next_run_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
      event.eventId(),event.type(),event.paymentIntentId(),payloadHash,"RECEIVED",OffsetDateTime.now(),event.stripeObjectId(),event.amount(),event.currency(),event.settlementAmount(),event.settlementCurrency(),event.fxRate(),event.reason(),0,OffsetDateTime.now()); }
    catch(DuplicateKeyException duplicate) { meters.counter("webhooks.received","outcome","duplicate").increment(); return Map.of("eventId",event.eventId(),"outcome","DUPLICATE_IGNORED"); }
    return applyWebhook(event);
  }

  private Map<String,Object> applyWebhook(ApiModels.Webhook event) {
    var payment=db.queryForList("select * from payments where stripe_payment_intent_id=?",event.paymentIntentId());
    if(payment.isEmpty()) {
      db.update("update webhook_events set status='PENDING_DEPENDENCY',note=?,attempt=attempt+1,next_run_at=? where event_id=?","payment not seen yet",OffsetDateTime.now().plusSeconds(2),event.eventId());
      meters.counter("webhooks.received","outcome","pending_dependency").increment();
      return Map.of("eventId",event.eventId(),"outcome","PENDING_DEPENDENCY");
    }
    Map<String,Object> p=payment.getFirst(); String paymentId=(String)p.get("ID"); String currency=event.currency()==null?(String)p.get("CURRENCY"):event.currency();
    String outcome;
    switch(event.type()) {
      case "payment_intent.succeeded" -> outcome=transition(paymentId,30,"CAPTURED",()->journal("CAPTURE",paymentId,event.paymentIntentId(),currency,event.amount(),new Line("stripe_receivable",event.amount()),new Line("customer_funds",-event.amount())));
      case "charge.refunded" -> { enforceRefundLimit(paymentId,((Number)p.get("AMOUNT")).longValue(),event.amount()); journal("REFUND",paymentId,event.stripeObjectId(),currency,event.amount(),new Line("refunds",event.amount()),new Line("stripe_receivable",-event.amount())); outcome="APPLIED"; }
      case "charge.dispute.created" -> { journal("DISPUTE",paymentId,event.stripeObjectId(),currency,event.amount(),new Line("dispute_loss",event.amount()),new Line("stripe_receivable",-event.amount())); outcome="APPLIED"; }
      case "charge.dispute.closed" -> { journal("DISPUTE_REVERSAL",paymentId,event.stripeObjectId(),currency,event.amount(),new Line("stripe_receivable",event.amount()),new Line("dispute_loss",-event.amount())); outcome="APPLIED"; }
      case "fee.adjusted" -> { journal("FEE_ADJUSTMENT",paymentId,event.stripeObjectId(),currency,event.amount(),new Line("processor_fees",event.amount()),new Line("stripe_receivable",-event.amount())); outcome="APPLIED"; }
      case "balance.available" -> { postFxSettlement(paymentId,event); outcome="APPLIED"; }
      default -> outcome="ERROR_FATAL_UNKNOWN_EVENT_TYPE";
    }
    String finalStatus=outcome.startsWith("ERROR_FATAL")?"ERROR_FATAL":"PROCESSED";
    db.update("update webhook_events set status=?,processed_at=?,note=?,attempt=attempt+1,next_run_at=null where event_id=?",finalStatus,OffsetDateTime.now(),outcome,event.eventId());
    audit("WEBHOOK_PROCESSED",paymentId,event.type()+":"+outcome,event.eventId());
    meters.counter("webhooks.received","outcome",outcome.toLowerCase()).increment();
    log.info("event=webhook_processed event_id={} payment_id={} stripe_object_id={} outcome={}",event.eventId(),paymentId,event.stripeObjectId(),outcome);
    return Map.of("eventId",event.eventId(),"outcome",outcome);
  }

  private void enforceRefundLimit(String paymentId,long paymentAmount,long nextRefund) {
    Long refunded=db.queryForObject("select coalesce(sum(amount),0) from ledger_entries where ref_id=? and entry_type='REFUND' and account_id='refunds'",Long.class,paymentId);
    if((refunded==null?0:refunded)+nextRefund>paymentAmount) throw new Conflict("REFUND_EXCEEDS_CAPTURED_AMOUNT");
  }

  private void postFxSettlement(String paymentId,ApiModels.Webhook event) {
    if(event.settlementAmount()==null||event.settlementCurrency()==null) throw new IllegalArgumentException("settlementAmount and settlementCurrency required");
    journal("FX_ORDER_CLEARING",paymentId,event.stripeObjectId(),event.currency(),event.amount(),new Line("fx_clearing",event.amount()),new Line("stripe_receivable",-event.amount()));
    journal("FX_SETTLEMENT",paymentId,event.stripeObjectId(),event.settlementCurrency(),event.settlementAmount(),new Line("processor_cash",event.settlementAmount()),new Line("fx_clearing",-event.settlementAmount()));
    audit("FX_RATE_APPLIED",paymentId,"rate="+event.fxRate()+",order="+event.amount()+event.currency()+",settlement="+event.settlementAmount()+event.settlementCurrency(),event.eventId());
  }

  @Scheduled(fixedDelayString="${app.webhooks.replay-ms:2000}")
  @Transactional
  public void replayPendingWebhooks() {
    var rows=db.queryForList("select * from webhook_events where status='PENDING_DEPENDENCY' and next_run_at<=? and attempt<10 order by received_at fetch first 20 rows only",OffsetDateTime.now());
    for(var w:rows) applyWebhook(new ApiModels.Webhook((String)w.get("EVENT_ID"),(String)w.get("EVENT_TYPE"),(String)w.get("OBJECT_ID"),(String)w.get("STRIPE_OBJECT_ID"),((Number)w.get("AMOUNT")).longValue(),(String)w.get("CURRENCY"),(String)w.get("REASON"),w.get("SETTLEMENT_AMOUNT")==null?null:((Number)w.get("SETTLEMENT_AMOUNT")).longValue(),(String)w.get("SETTLEMENT_CURRENCY"),(String)w.get("FX_RATE")));
  }

  private String transition(String id,int targetRank,String status,Runnable effect) {
    int updated=db.update("update payments set status=?,state_rank=?,failure_reason=null where id=? and state_rank<?",status,targetRank,id,targetRank);
    if(updated==0) return "STALE_EVENT_IGNORED";
    effect.run(); return "APPLIED";
  }

  private record Line(String account,long amount) {}
  private void journal(String type,String ref,String stripeId,String currency,long amount,Line debit,Line credit) {
    String jid="jrnl_"+UUID.randomUUID().toString().substring(0,12); OffsetDateTime now=OffsetDateTime.now();
    insertLine(jid,debit,type,ref,stripeId,currency,now); insertLine(jid,credit,type,ref,stripeId,currency,now);
    if(debit.amount+credit.amount!=0) throw new IllegalStateException("unbalanced journal");
  }
  private void insertLine(String jid,Line line,String type,String ref,String stripeId,String currency,OffsetDateTime now) {
    db.update("insert into ledger_entries values(?,?,?,?,?,?,?,?,?,?)","le_"+UUID.randomUUID().toString().substring(0,12),jid,line.account,currency,line.amount,type,ref,stripeId,type+" for "+ref,now);
    int updated=db.update("update balance_projection set balance=balance+?,version=version+1,updated_at=? where account_id=? and currency=?",line.amount,now,line.account,currency);
    if(updated==0) db.update("insert into balance_projection values(?,?,?,?,?)",line.account,currency,line.amount,1,now);
  }
  private void audit(String type,String aggregate,String details,String correlation) {
    db.update("insert into audit_events values(?,?,?,?,?,?)","aud_"+UUID.randomUUID().toString().substring(0,12),type,aggregate,details,correlation,OffsetDateTime.now());
  }

  public List<Map<String,Object>> payments(){ return db.queryForList("select * from payments order by created_at desc"); }
  public List<Map<String,Object>> ledger(){ return db.queryForList("select * from ledger_entries order by created_at desc"); }
  public List<Map<String,Object>> balances(){ return db.queryForList("select * from balance_projection order by account_id,currency"); }
  public Map<String,Object> support(String paymentId){
    var p=db.queryForMap("select * from payments where id=?",paymentId);
    return Map.of("payment",p,"possibleDuplicateCharges",db.queryForList("select id,stripe_payment_intent_id,stripe_charge_id,amount,currency,created_at from payments where customer_id=? and order_id=? and id<>? order by created_at",p.get("CUSTOMER_ID"),p.get("ORDER_ID"),paymentId),"ledgerEntries",db.queryForList("select * from ledger_entries where ref_id=? order by created_at",paymentId),
      "webhooks",db.queryForList("select * from webhook_events where object_id=? order by received_at",p.get("STRIPE_PAYMENT_INTENT_ID")),
      "auditTrail",db.queryForList("select * from audit_events where aggregate_id=? order by created_at",paymentId));
  }

  public static class Conflict extends RuntimeException { Conflict(String m){super(m);} }
}
