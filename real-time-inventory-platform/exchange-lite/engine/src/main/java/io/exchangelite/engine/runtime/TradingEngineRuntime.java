package io.exchangelite.engine.runtime;

import io.exchangelite.common.domain.CancelRequest;
import io.exchangelite.common.domain.ExecutionReport;
import io.exchangelite.common.domain.OrderRequest;
import io.exchangelite.common.domain.Trade;
import io.exchangelite.common.metrics.ExchangeMetrics;
import io.exchangelite.common.metrics.MetricsSnapshot;
import io.exchangelite.engine.core.InMemoryPersistenceStore;
import io.exchangelite.engine.core.MarketManager;
import io.exchangelite.engine.core.MatchingEngine;
import io.exchangelite.engine.core.Order;
import io.exchangelite.engine.core.OrderBookLevel;
import io.exchangelite.engine.core.OrderBookSnapshot;
import io.exchangelite.engine.core.PersistenceStore;
import io.exchangelite.engine.core.RiskDecision;
import io.exchangelite.engine.core.RiskEngine;
import io.exchangelite.engine.core.SessionManager;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TradingEngineRuntime {
    private final EngineConfig config;
    private final MatchingEngine matchingEngine = new MatchingEngine();
    private final RiskEngine riskEngine;
    private final MarketManager marketManager;
    private final SessionManager sessionManager = new SessionManager();
    private final PersistenceStore persistenceStore = new InMemoryPersistenceStore(10_000);
    private final ExchangeMetrics metrics = new ExchangeMetrics();
    private final AtomicBoolean healthy = new AtomicBoolean(true);

    public TradingEngineRuntime(EngineConfig config) {
        this.config = config;
        this.riskEngine = new RiskEngine(config.maxOrderQuantity(), config.maxOrderNotionalTicks());
        this.marketManager = new MarketManager(config.markets());
    }

    public ExecutionReport submitOrder(OrderRequest request) {
        long start = System.nanoTime();
        if (!marketManager.isOpen(request.market())) {
            ExecutionReport report = ExecutionReport.rejected(request, "market closed or unknown");
            metrics.recordRejectedOrder();
            persistenceStore.append(report);
            return report;
        }

        RiskDecision riskDecision = riskEngine.evaluate(request);
        if (!riskDecision.accepted()) {
            ExecutionReport report = ExecutionReport.rejected(request, riskDecision.reason());
            metrics.recordRejectedOrder();
            persistenceStore.append(report);
            return report;
        }

        ExecutionReport report = matchingEngine.submit(request);
        persistenceStore.append(report);
        if (report.status().name().equals("REJECTED")) {
            metrics.recordRejectedOrder();
        } else {
            metrics.recordAcceptedOrder(System.nanoTime() - start);
        }
        report.trades().forEach(ignored -> metrics.recordTrade());
        return report;
    }

    public ExecutionReport cancel(CancelRequest request) {
        ExecutionReport report = matchingEngine.cancel(request);
        persistenceStore.append(report);
        if (report.status().name().equals("CANCELLED")) {
            metrics.recordCancelledOrder();
        }
        return report;
    }

    public long registerSession() {
        return sessionManager.register();
    }

    public void unregisterSession(long sessionId) {
        sessionManager.unregister(sessionId);
    }

    public ExchangeMetrics metrics() {
        return metrics;
    }

    public void markUnhealthy() {
        healthy.set(false);
    }

    public String healthJson() {
        return "{"
                + "\"status\":" + EngineJson.quote(healthy.get() ? "UP" : "DOWN") + ","
                + "\"timestamp\":" + EngineJson.quote(Instant.now().toString()) + ","
                + "\"openOrders\":" + matchingEngine.openOrderCount() + ","
                + "\"sessions\":" + sessionManager.activeSessions()
                + "}";
    }

    public String statsJson() {
        MetricsSnapshot snapshot = metrics.snapshot();
        return "{"
                + "\"openOrders\":" + matchingEngine.openOrderCount() + ","
                + "\"ordersAccepted\":" + snapshot.ordersAccepted() + ","
                + "\"ordersRejected\":" + snapshot.ordersRejected() + ","
                + "\"ordersCancelled\":" + snapshot.ordersCancelled() + ","
                + "\"tradesExecuted\":" + snapshot.tradesExecuted() + ","
                + "\"bytesRead\":" + snapshot.bytesRead() + ","
                + "\"ipcCommands\":" + snapshot.ipcCommands()
                + "}";
    }

    public String ordersJson() {
        List<String> orders = matchingEngine.openOrders().stream()
                .map(this::orderJson)
                .toList();
        return "{\"orders\":" + EngineJson.array(orders) + ",\"recentReports\":" + reportArrayJson() + "}";
    }

    public String marketsJson() {
        List<String> books = matchingEngine.snapshots(5).stream().map(this::bookJson).toList();
        List<String> markets = marketManager.openMarkets().stream().map(EngineJson::quote).toList();
        return "{\"openMarkets\":" + EngineJson.array(markets) + ",\"books\":" + EngineJson.array(books) + "}";
    }

    public String sessionsJson() {
        return sessionManager.json();
    }

    public String riskJson() {
        return riskEngine.json();
    }

    public String configJson() {
        return config.json();
    }

    public String reloadConfigJson() {
        return "{\"reloaded\":true,\"strategy\":\"immutable config in milestone 1; dynamic sources attach here\"}";
    }

    public String heapJson() {
        MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
        return "{"
                + "\"heapUsed\":" + bean.getHeapMemoryUsage().getUsed() + ","
                + "\"heapCommitted\":" + bean.getHeapMemoryUsage().getCommitted() + ","
                + "\"nonHeapUsed\":" + bean.getNonHeapMemoryUsage().getUsed() + ","
                + "\"nonHeapCommitted\":" + bean.getNonHeapMemoryUsage().getCommitted()
                + "}";
    }

    public String threadsJson() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] infos = bean.dumpAllThreads(false, false);
        List<String> threads = java.util.Arrays.stream(infos)
                .limit(32)
                .map(info -> "{"
                        + "\"id\":" + info.getThreadId() + ","
                        + "\"name\":" + EngineJson.quote(info.getThreadName()) + ","
                        + "\"state\":" + EngineJson.quote(info.getThreadState().name())
                        + "}")
                .toList();
        return "{"
                + "\"threadCount\":" + bean.getThreadCount() + ","
                + "\"peakThreadCount\":" + bean.getPeakThreadCount() + ","
                + "\"sample\":" + EngineJson.array(threads)
                + "}";
    }

    public String metricsJson() {
        MetricsSnapshot snapshot = metrics.snapshot();
        return "{"
                + "\"exchange_orders_accepted_total\":" + snapshot.ordersAccepted() + ","
                + "\"exchange_orders_rejected_total\":" + snapshot.ordersRejected() + ","
                + "\"exchange_orders_cancelled_total\":" + snapshot.ordersCancelled() + ","
                + "\"exchange_trades_executed_total\":" + snapshot.tradesExecuted() + ","
                + "\"exchange_matching_latency_nanos_total\":" + snapshot.matchingLatencyNanos()
                + "}";
    }

    public String prometheusText() {
        MetricsSnapshot snapshot = metrics.snapshot();
        return "# HELP exchange_orders_accepted_total Accepted orders\n"
                + "# TYPE exchange_orders_accepted_total counter\n"
                + "exchange_orders_accepted_total " + snapshot.ordersAccepted() + "\n"
                + "# HELP exchange_orders_rejected_total Rejected orders\n"
                + "# TYPE exchange_orders_rejected_total counter\n"
                + "exchange_orders_rejected_total " + snapshot.ordersRejected() + "\n"
                + "# HELP exchange_trades_executed_total Executed trades\n"
                + "# TYPE exchange_trades_executed_total counter\n"
                + "exchange_trades_executed_total " + snapshot.tradesExecuted() + "\n";
    }

    public String executionReportJson(ExecutionReport report) {
        return "{"
                + "\"market\":" + EngineJson.quote(report.market()) + ","
                + "\"clientOrderId\":" + EngineJson.quote(report.clientOrderId()) + ","
                + "\"accountId\":" + EngineJson.quote(report.accountId()) + ","
                + "\"status\":" + EngineJson.quote(report.status().name()) + ","
                + "\"filledQuantity\":" + report.filledQuantity() + ","
                + "\"remainingQuantity\":" + report.remainingQuantity() + ","
                + "\"averagePriceTicks\":" + report.averagePriceTicks() + ","
                + "\"reason\":" + EngineJson.quote(report.reason()) + ","
                + "\"trades\":" + EngineJson.array(report.trades().stream().map(this::tradeJson).toList())
                + "}";
    }

    private String reportArrayJson() {
        return EngineJson.array(persistenceStore.recentReports().stream()
                .map(this::executionReportJson)
                .toList());
    }

    private String orderJson(Order order) {
        return "{"
                + "\"market\":" + EngineJson.quote(order.market()) + ","
                + "\"clientOrderId\":" + EngineJson.quote(order.clientOrderId()) + ","
                + "\"accountId\":" + EngineJson.quote(order.accountId()) + ","
                + "\"side\":" + EngineJson.quote(order.side().name()) + ","
                + "\"type\":" + EngineJson.quote(order.type().name()) + ","
                + "\"priceTicks\":" + order.priceTicks() + ","
                + "\"remainingQuantity\":" + order.remainingQuantity() + ","
                + "\"sequence\":" + order.sequence()
                + "}";
    }

    private String bookJson(OrderBookSnapshot snapshot) {
        return "{"
                + "\"market\":" + EngineJson.quote(snapshot.market()) + ","
                + "\"openOrderCount\":" + snapshot.openOrderCount() + ","
                + "\"bids\":" + EngineJson.array(snapshot.bids().stream().map(this::levelJson).toList()) + ","
                + "\"asks\":" + EngineJson.array(snapshot.asks().stream().map(this::levelJson).toList())
                + "}";
    }

    private String levelJson(OrderBookLevel level) {
        return "{"
                + "\"priceTicks\":" + level.priceTicks() + ","
                + "\"visibleQuantity\":" + level.visibleQuantity() + ","
                + "\"orderCount\":" + level.orderCount()
                + "}";
    }

    private String tradeJson(Trade trade) {
        return "{"
                + "\"market\":" + EngineJson.quote(trade.market()) + ","
                + "\"buyOrderId\":" + EngineJson.quote(trade.buyOrderId()) + ","
                + "\"sellOrderId\":" + EngineJson.quote(trade.sellOrderId()) + ","
                + "\"priceTicks\":" + trade.priceTicks() + ","
                + "\"quantity\":" + trade.quantity() + ","
                + "\"executedAtNanos\":" + trade.executedAtNanos()
                + "}";
    }
}
