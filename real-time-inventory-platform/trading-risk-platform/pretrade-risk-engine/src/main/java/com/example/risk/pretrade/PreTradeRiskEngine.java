package com.example.risk.pretrade;

import com.example.risk.pretrade.Models.AccountSummary;
import com.example.risk.pretrade.Models.AuditEvent;
import com.example.risk.pretrade.Models.CircuitBreakerRequest;
import com.example.risk.pretrade.Models.EngineState;
import com.example.risk.pretrade.Models.EventType;
import com.example.risk.pretrade.Models.KillSwitchRequest;
import com.example.risk.pretrade.Models.LimitsSnapshot;
import com.example.risk.pretrade.Models.MarketPrice;
import com.example.risk.pretrade.Models.MarketPriceRequest;
import com.example.risk.pretrade.Models.OrderDecision;
import com.example.risk.pretrade.Models.OrderRequest;
import com.example.risk.pretrade.Models.OrderStatus;
import com.example.risk.pretrade.Models.PositionSnapshot;
import com.example.risk.pretrade.Models.RiskCheckResult;
import com.example.risk.pretrade.Models.ScenarioResult;
import com.example.risk.pretrade.Models.Side;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class PreTradeRiskEngine {
    private static final BigDecimal ACCOUNT_BUYING_POWER = Models.money(new BigDecimal("1000000"));
    private static final long MAX_ORDER_QUANTITY = 10_000;
    private static final BigDecimal MAX_ORDER_NOTIONAL = Models.money(new BigDecimal("750000"));
    private static final long POSITION_LIMIT = 5_000;
    private static final BigDecimal PRICE_COLLAR_PERCENT = new BigDecimal("0.10");
    private static final Duration MARKET_DATA_STALE_AFTER = Duration.ofMinutes(5);
    private static final int RECENT_LIMIT = 20;
    private static final int AUDIT_LIMIT = 200;

    private final Clock clock;
    private final ConcurrentMap<String, AccountRisk> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Position> positions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MarketPrice> marketData = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> killSwitches = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, OpenOrder> openOrders = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ReentrantLock> accountLocks = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<OrderDecision> recentDecisions = new ConcurrentLinkedDeque<>();
    private final List<AuditEvent> auditTrail = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong();
    private volatile boolean circuitBreakerOpen;

    public PreTradeRiskEngine() {
        this(Clock.systemUTC());
    }

    PreTradeRiskEngine(Clock clock) {
        this.clock = clock;
        reset();
    }

    public synchronized EngineState reset() {
        accounts.clear();
        positions.clear();
        marketData.clear();
        killSwitches.clear();
        openOrders.clear();
        accountLocks.clear();
        recentDecisions.clear();
        auditTrail.clear();
        sequence.set(0);
        circuitBreakerOpen = false;

        accounts.put("ACCT-DEMO", new AccountRisk(ACCOUNT_BUYING_POWER));
        setSeedPrice("MSFT", "410.25");
        setSeedPrice("AAPL", "225.10");
        setSeedPrice("NVDA", "118.60");
        audit(EventType.ENGINE_RESET, "ENGINE", "Engine state rebuilt from seed data", Map.of());
        return state();
    }

    public OrderDecision submit(OrderRequest rawRequest) {
        OrderRequest request = normalize(rawRequest);
        long started = System.nanoTime();
        ReentrantLock lock = accountLocks.computeIfAbsent(request.account(), ignored -> new ReentrantLock());
        lock.lock();
        try {
            AccountRisk account = accounts.computeIfAbsent(request.account(), ignored -> new AccountRisk(ACCOUNT_BUYING_POWER));
            Position position = positions.computeIfAbsent(positionKey(request.account(), request.symbol()),
                    ignored -> new Position(request.account(), request.symbol()));

            List<RiskCheckResult> checks = new ArrayList<>();
            checks.add(runCheck("Kill switch", () -> killSwitchCheck(request)));
            checks.add(runCheck("Circuit breaker", () -> circuitBreakerCheck()));
            checks.add(runCheck("Market data freshness", () -> marketDataFreshnessCheck(request.symbol())));
            checks.add(runCheck("Max quantity", () -> maxQuantityCheck(request)));
            checks.add(runCheck("Max notional", () -> maxNotionalCheck(request)));
            checks.add(runCheck("Price collar", () -> priceCollarCheck(request)));
            checks.add(runCheck("Buying power reservation", () -> buyingPowerCheck(account, request)));
            checks.add(runCheck("Position limit", () -> positionLimitCheck(position, request)));

            boolean accepted = checks.stream().allMatch(RiskCheckResult::passed);
            String orderId = UUID.randomUUID().toString();
            String reason = accepted
                    ? "Accepted and exposure reserved atomically"
                    : checks.stream()
                    .filter(check -> !check.passed())
                    .map(RiskCheckResult::detail)
                    .findFirst()
                    .orElse("Rejected by risk policy");

            OrderDecision decision = new OrderDecision(
                    orderId,
                    request.clOrdId(),
                    request.account(),
                    request.symbol(),
                    request.side(),
                    request.quantity(),
                    request.price(),
                    request.notional(),
                    accepted ? OrderStatus.ACCEPTED : OrderStatus.REJECTED,
                    reason,
                    List.copyOf(checks),
                    microsSince(started),
                    Instant.now(clock));

            if (accepted) {
                reserve(account, position, request);
                OpenOrder openOrder = new OpenOrder(orderId, request);
                openOrders.put(orderId, openOrder);
                audit(EventType.ORDER_ACCEPTED, request.account(), "Order accepted: " + request.clOrdId(), orderData(decision));
                audit(EventType.EXPOSURE_RESERVED, position.key(), "Exposure reserved in memory", Map.of(
                        "orderId", orderId,
                        "account", request.account(),
                        "symbol", request.symbol(),
                        "side", request.side().name(),
                        "quantity", request.quantity(),
                        "notional", request.notional()));
                if (request.autoFill()) {
                    applyFill(openOrder);
                    openOrders.remove(orderId);
                }
            } else {
                audit(EventType.ORDER_REJECTED, request.account(), "Order rejected: " + reason, orderData(decision));
            }

            rememberDecision(decision);
            return decision;
        } finally {
            lock.unlock();
        }
    }

    public EngineState fill(String orderId) {
        OpenOrder openOrder = openOrders.get(orderId);
        if (openOrder == null) {
            throw new IllegalArgumentException("No open order found for " + orderId);
        }

        ReentrantLock lock = accountLocks.computeIfAbsent(openOrder.request().account(), ignored -> new ReentrantLock());
        lock.lock();
        try {
            OpenOrder current = openOrders.get(orderId);
            if (current == null) {
                return state();
            }
            applyFill(current);
            openOrders.remove(orderId);
            return state();
        } finally {
            lock.unlock();
        }
    }

    public EngineState updateMarketPrice(MarketPriceRequest request) {
        MarketPrice price = new MarketPrice(request.symbol(), request.price(), Instant.now(clock));
        marketData.put(request.symbol(), price);
        audit(EventType.MARKET_PRICE_CHANGED, request.symbol(), "Market price updated", Map.of(
                "symbol", request.symbol(),
                "price", request.price()));
        return state();
    }

    public EngineState setKillSwitch(KillSwitchRequest request) {
        String key = killSwitchKey(request.scope(), request.key());
        killSwitches.put(key, request.enabled());
        audit(EventType.KILL_SWITCH_CHANGED, key, "Kill switch " + (request.enabled() ? "enabled" : "disabled"), Map.of(
                "scope", request.scope(),
                "key", request.key(),
                "enabled", request.enabled(),
                "reason", request.reason()));
        return state();
    }

    public EngineState setCircuitBreaker(CircuitBreakerRequest request) {
        circuitBreakerOpen = request.open();
        audit(EventType.CIRCUIT_BREAKER_CHANGED, "ENGINE", "Circuit breaker " + (request.open() ? "opened" : "closed"), Map.of(
                "open", request.open(),
                "reason", request.reason()));
        return state();
    }

    public ScenarioResult runScenario(String name) {
        String scenario = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return switch (scenario) {
            case "accept" -> acceptedScenario();
            case "reject" -> rejectedScenario();
            case "race" -> raceScenario();
            case "kill" -> killSwitchScenario();
            case "failure" -> failureScenario();
            case "pnl" -> pnlScenario();
            default -> throw new IllegalArgumentException("Unknown scenario: " + name);
        };
    }

    public EngineState state() {
        Map<String, AccountSummary> accountSummaries = new LinkedHashMap<>();
        accounts.keySet().stream().sorted().forEach(account -> {
            AccountRisk accountRisk = accounts.get(account);
            BigDecimal unrealized = positions.values().stream()
                    .filter(position -> position.account().equals(account))
                    .map(this::unrealizedPnl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            accountSummaries.put(account, new AccountSummary(
                    account,
                    accountRisk.buyingPowerLimit(),
                    Models.money(accountRisk.reservedNotional()),
                    Models.money(accountRisk.realizedPnl()),
                    Models.money(unrealized),
                    Models.money(accountRisk.buyingPowerLimit().subtract(accountRisk.reservedNotional()))));
        });

        List<PositionSnapshot> positionSnapshots = positions.values().stream()
                .sorted(Comparator.comparing(Position::account).thenComparing(Position::symbol))
                .map(this::snapshot)
                .toList();

        Map<String, MarketPrice> marketSnapshot = new LinkedHashMap<>();
        marketData.keySet().stream().sorted().forEach(key -> marketSnapshot.put(key, marketData.get(key)));

        Map<String, Boolean> killSnapshot = new LinkedHashMap<>();
        killSwitches.keySet().stream().sorted().forEach(key -> killSnapshot.put(key, killSwitches.get(key)));

        List<AuditEvent> auditSnapshot;
        synchronized (auditTrail) {
            auditSnapshot = auditTrail.stream()
                    .sorted(Comparator.comparing(AuditEvent::sequence).reversed())
                    .limit(100)
                    .toList();
        }

        return new EngineState(
                accountSummaries,
                positionSnapshots,
                marketSnapshot,
                killSnapshot,
                circuitBreakerOpen,
                recentDecisions.stream().limit(RECENT_LIMIT).toList(),
                auditSnapshot,
                new LimitsSnapshot(
                        ACCOUNT_BUYING_POWER,
                        MAX_ORDER_QUANTITY,
                        MAX_ORDER_NOTIONAL,
                        POSITION_LIMIT,
                        PRICE_COLLAR_PERCENT,
                        MARKET_DATA_STALE_AFTER.toSeconds()));
    }

    private ScenarioResult acceptedScenario() {
        reset();
        OrderDecision decision = submit(new OrderRequest("ACC-1001", "ACCT-DEMO", "MSFT", Side.BUY, 100,
                new BigDecimal("410.25"), false));
        return new ScenarioResult(
                "accept",
                "Normal order path: FIX or JSON order becomes a normalized model, every check passes, then exposure is reserved in memory.",
                List.of(decision),
                state());
    }

    private ScenarioResult rejectedScenario() {
        reset();
        OrderDecision decision = submit(new OrderRequest("REJ-1001", "ACCT-DEMO", "MSFT", Side.BUY, 20_000,
                new BigDecimal("410.25"), false));
        return new ScenarioResult(
                "reject",
                "Fat-finger rejection: the order is blocked before routing because quantity and notional exceed the risk limits.",
                List.of(decision),
                state());
    }

    private ScenarioResult killSwitchScenario() {
        reset();
        setKillSwitch(new KillSwitchRequest("ACCOUNT", "ACCT-DEMO", true, "desk kill switch test"));
        OrderDecision decision = submit(new OrderRequest("KILL-1001", "ACCT-DEMO", "MSFT", Side.BUY, 100,
                new BigDecimal("410.25"), false));
        return new ScenarioResult(
                "kill",
                "Kill switch path: the account-level switch is checked first and rejects new risk-increasing orders immediately.",
                List.of(decision),
                state());
    }

    private ScenarioResult failureScenario() {
        reset();
        setCircuitBreaker(new CircuitBreakerRequest(true, "market data sequence gap"));
        OrderDecision decision = submit(new OrderRequest("FAIL-1001", "ACCT-DEMO", "MSFT", Side.BUY, 100,
                new BigDecimal("410.25"), false));
        return new ScenarioResult(
                "failure",
                "Production failure drill: when market data or a downstream dependency is unsafe, the engine opens the breaker and fails closed.",
                List.of(decision),
                state());
    }

    private ScenarioResult pnlScenario() {
        reset();
        OrderDecision decision = submit(new OrderRequest("PNL-1001", "ACCT-DEMO", "MSFT", Side.BUY, 100,
                new BigDecimal("410.25"), true));
        updateMarketPrice(new MarketPriceRequest("MSFT", new BigDecimal("418.75")));
        return new ScenarioResult(
                "pnl",
                "Real-time P&L path: fills change the position and market ticks re-mark unrealized P&L immediately.",
                List.of(decision),
                state());
    }

    private ScenarioResult raceScenario() {
        reset();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<OrderDecision> first = racingOrder(start, "RACE-700K-A");
            Callable<OrderDecision> second = racingOrder(start, "RACE-700K-B");
            Future<OrderDecision> firstFuture = executor.submit(first);
            Future<OrderDecision> secondFuture = executor.submit(second);
            start.countDown();
            List<OrderDecision> decisions = List.of(firstFuture.get(5, TimeUnit.SECONDS), secondFuture.get(5, TimeUnit.SECONDS));
            return new ScenarioResult(
                    "race",
                    "Race-condition demo: two concurrent orders both fit individually, but the account lock makes check plus reserve atomic, so only one can consume the remaining buying power.",
                    decisions,
                    state());
        } catch (Exception ex) {
            throw new IllegalStateException("Race demo failed", ex);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<OrderDecision> racingOrder(CountDownLatch start, String clOrdId) {
        return () -> {
            start.await(5, TimeUnit.SECONDS);
            return submit(new OrderRequest(clOrdId, "ACCT-DEMO", "MSFT", Side.BUY, 1700,
                    new BigDecimal("410.25"), false));
        };
    }

    private OrderRequest normalize(OrderRequest request) {
        Objects.requireNonNull(request, "request is required");
        return new OrderRequest(
                request.clOrdId(),
                request.account(),
                request.symbol(),
                request.side(),
                request.quantity(),
                request.price(),
                request.autoFill());
    }

    private CheckOutcome killSwitchCheck(OrderRequest request) {
        List<String> active = List.of(
                killSwitchKey("GLOBAL", "*"),
                killSwitchKey("ACCOUNT", request.account()),
                killSwitchKey("SYMBOL", request.symbol()),
                killSwitchKey("ACCOUNT_SYMBOL", request.account() + "|" + request.symbol()));
        String activeKey = active.stream()
                .filter(key -> Boolean.TRUE.equals(killSwitches.get(key)))
                .findFirst()
                .orElse(null);
        return activeKey == null
                ? CheckOutcome.pass("No active kill switch")
                : CheckOutcome.fail("Active kill switch: " + activeKey);
    }

    private CheckOutcome circuitBreakerCheck() {
        return circuitBreakerOpen
                ? CheckOutcome.fail("Circuit breaker is open; fail closed")
                : CheckOutcome.pass("Circuit breaker is closed");
    }

    private CheckOutcome marketDataFreshnessCheck(String symbol) {
        MarketPrice price = marketData.get(symbol);
        if (price == null) {
            return CheckOutcome.fail("No market data for " + symbol);
        }
        long ageSeconds = Duration.between(price.updatedAt(), Instant.now(clock)).toSeconds();
        if (ageSeconds > MARKET_DATA_STALE_AFTER.toSeconds()) {
            return CheckOutcome.fail("Market data is stale: " + ageSeconds + " seconds old");
        }
        return CheckOutcome.pass("Market data age " + ageSeconds + " seconds");
    }

    private CheckOutcome maxQuantityCheck(OrderRequest request) {
        return request.quantity() <= MAX_ORDER_QUANTITY
                ? CheckOutcome.pass("Quantity " + request.quantity() + " <= " + MAX_ORDER_QUANTITY)
                : CheckOutcome.fail("Quantity " + request.quantity() + " exceeds " + MAX_ORDER_QUANTITY);
    }

    private CheckOutcome maxNotionalCheck(OrderRequest request) {
        return request.notional().compareTo(MAX_ORDER_NOTIONAL) <= 0
                ? CheckOutcome.pass("Notional " + request.notional() + " <= " + MAX_ORDER_NOTIONAL)
                : CheckOutcome.fail("Notional " + request.notional() + " exceeds " + MAX_ORDER_NOTIONAL);
    }

    private CheckOutcome priceCollarCheck(OrderRequest request) {
        MarketPrice price = marketData.get(request.symbol());
        if (price == null) {
            return CheckOutcome.fail("Cannot apply price collar without market data");
        }
        BigDecimal lower = price.price().multiply(BigDecimal.ONE.subtract(PRICE_COLLAR_PERCENT));
        BigDecimal upper = price.price().multiply(BigDecimal.ONE.add(PRICE_COLLAR_PERCENT));
        boolean inside = request.price().compareTo(lower) >= 0 && request.price().compareTo(upper) <= 0;
        return inside
                ? CheckOutcome.pass("Price inside 10% collar around " + price.price())
                : CheckOutcome.fail("Price " + request.price() + " outside collar " + Models.money(lower) + " - " + Models.money(upper));
    }

    private CheckOutcome buyingPowerCheck(AccountRisk account, OrderRequest request) {
        BigDecimal projected = account.reservedNotional().add(request.notional());
        return projected.compareTo(account.buyingPowerLimit()) <= 0
                ? CheckOutcome.pass("Projected reserved exposure " + Models.money(projected))
                : CheckOutcome.fail("Projected reserved exposure " + Models.money(projected)
                + " exceeds buying power " + account.buyingPowerLimit());
    }

    private CheckOutcome positionLimitCheck(Position position, OrderRequest request) {
        long projected = request.side() == Side.BUY
                ? position.netQuantity() + position.openBuyQuantity() + request.quantity()
                : position.netQuantity() - position.openSellQuantity() - request.quantity();
        return Math.abs(projected) <= POSITION_LIMIT
                ? CheckOutcome.pass("Projected position " + projected + " within " + POSITION_LIMIT)
                : CheckOutcome.fail("Projected position " + projected + " exceeds limit " + POSITION_LIMIT);
    }

    private RiskCheckResult runCheck(String name, Supplier<CheckOutcome> check) {
        long started = System.nanoTime();
        CheckOutcome outcome = check.get();
        return new RiskCheckResult(name, outcome.passed(), outcome.detail(), microsSince(started));
    }

    private void reserve(AccountRisk account, Position position, OrderRequest request) {
        account.addReserved(request.notional());
        position.addReservation(request);
    }

    private void applyFill(OpenOrder openOrder) {
        OrderRequest request = openOrder.request();
        AccountRisk account = accounts.computeIfAbsent(request.account(), ignored -> new AccountRisk(ACCOUNT_BUYING_POWER));
        Position position = positions.computeIfAbsent(positionKey(request.account(), request.symbol()),
                ignored -> new Position(request.account(), request.symbol()));
        account.releaseReserved(request.notional());
        BigDecimal realizedDelta = position.applyFill(request);
        account.addRealized(realizedDelta);
        audit(EventType.FILL_APPLIED, position.key(), "Fill applied and position marked", Map.of(
                "orderId", openOrder.orderId(),
                "account", request.account(),
                "symbol", request.symbol(),
                "side", request.side().name(),
                "quantity", request.quantity(),
                "price", request.price(),
                "realizedDelta", Models.money(realizedDelta)));
    }

    private PositionSnapshot snapshot(Position position) {
        MarketPrice price = marketData.get(position.symbol());
        BigDecimal marketPrice = price == null ? BigDecimal.ZERO : price.price();
        return new PositionSnapshot(
                position.account(),
                position.symbol(),
                position.netQuantity(),
                position.openBuyQuantity(),
                position.openSellQuantity(),
                Models.money(position.reservedNotional()),
                Models.money(position.averageCost()),
                Models.money(marketPrice),
                Models.money(position.realizedPnl()),
                Models.money(unrealizedPnl(position)));
    }

    private BigDecimal unrealizedPnl(Position position) {
        if (position.netQuantity() == 0) {
            return BigDecimal.ZERO;
        }
        MarketPrice price = marketData.get(position.symbol());
        if (price == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal quantity = BigDecimal.valueOf(Math.abs(position.netQuantity()));
        BigDecimal delta = position.netQuantity() > 0
                ? price.price().subtract(position.averageCost())
                : position.averageCost().subtract(price.price());
        return Models.money(delta.multiply(quantity));
    }

    private Map<String, Object> orderData(OrderDecision decision) {
        return Map.of(
                "orderId", decision.orderId(),
                "clOrdId", decision.clOrdId(),
                "account", decision.account(),
                "symbol", decision.symbol(),
                "side", decision.side().name(),
                "quantity", decision.quantity(),
                "notional", decision.notional(),
                "status", decision.status().name(),
                "reason", decision.reason(),
                "latencyMicros", decision.totalLatencyMicros());
    }

    private void audit(EventType type, String aggregateKey, String message, Map<String, Object> data) {
        AuditEvent event = new AuditEvent(sequence.incrementAndGet(), Instant.now(clock), type, aggregateKey, message, data);
        synchronized (auditTrail) {
            auditTrail.add(event);
            if (auditTrail.size() > AUDIT_LIMIT) {
                auditTrail.remove(0);
            }
        }
    }

    private void rememberDecision(OrderDecision decision) {
        recentDecisions.addFirst(decision);
        while (recentDecisions.size() > RECENT_LIMIT) {
            recentDecisions.pollLast();
        }
    }

    private void setSeedPrice(String symbol, String price) {
        marketData.put(symbol, new MarketPrice(symbol, Models.money(new BigDecimal(price)), Instant.now(clock)));
    }

    private static String positionKey(String account, String symbol) {
        return account + "|" + symbol;
    }

    private static String killSwitchKey(String scope, String key) {
        return scope.toUpperCase(Locale.ROOT) + ":" + (key == null || key.isBlank() ? "*" : key.toUpperCase(Locale.ROOT));
    }

    private static long microsSince(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startedNanos);
    }

    private record CheckOutcome(boolean passed, String detail) {
        static CheckOutcome pass(String detail) {
            return new CheckOutcome(true, detail);
        }

        static CheckOutcome fail(String detail) {
            return new CheckOutcome(false, detail);
        }
    }

    private record OpenOrder(String orderId, OrderRequest request) {
    }

    private static final class AccountRisk {
        private final BigDecimal buyingPowerLimit;
        private BigDecimal reservedNotional = BigDecimal.ZERO;
        private BigDecimal realizedPnl = BigDecimal.ZERO;

        private AccountRisk(BigDecimal buyingPowerLimit) {
            this.buyingPowerLimit = buyingPowerLimit;
        }

        private BigDecimal buyingPowerLimit() {
            return buyingPowerLimit;
        }

        private BigDecimal reservedNotional() {
            return reservedNotional;
        }

        private BigDecimal realizedPnl() {
            return realizedPnl;
        }

        private void addReserved(BigDecimal notional) {
            reservedNotional = Models.money(reservedNotional.add(notional));
        }

        private void releaseReserved(BigDecimal notional) {
            reservedNotional = Models.money(reservedNotional.subtract(notional).max(BigDecimal.ZERO));
        }

        private void addRealized(BigDecimal realizedDelta) {
            realizedPnl = Models.money(realizedPnl.add(realizedDelta));
        }
    }

    private static final class Position {
        private final String account;
        private final String symbol;
        private long netQuantity;
        private long openBuyQuantity;
        private long openSellQuantity;
        private BigDecimal reservedNotional = BigDecimal.ZERO;
        private BigDecimal averageCost = BigDecimal.ZERO;
        private BigDecimal realizedPnl = BigDecimal.ZERO;

        private Position(String account, String symbol) {
            this.account = account;
            this.symbol = symbol;
        }

        private String account() {
            return account;
        }

        private String symbol() {
            return symbol;
        }

        private String key() {
            return account + "|" + symbol;
        }

        private long netQuantity() {
            return netQuantity;
        }

        private long openBuyQuantity() {
            return openBuyQuantity;
        }

        private long openSellQuantity() {
            return openSellQuantity;
        }

        private BigDecimal reservedNotional() {
            return reservedNotional;
        }

        private BigDecimal averageCost() {
            return averageCost;
        }

        private BigDecimal realizedPnl() {
            return realizedPnl;
        }

        private void addReservation(OrderRequest request) {
            if (request.side() == Side.BUY) {
                openBuyQuantity += request.quantity();
            } else {
                openSellQuantity += request.quantity();
            }
            reservedNotional = Models.money(reservedNotional.add(request.notional()));
        }

        private BigDecimal applyFill(OrderRequest request) {
            if (request.side() == Side.BUY) {
                openBuyQuantity = Math.max(0, openBuyQuantity - request.quantity());
            } else {
                openSellQuantity = Math.max(0, openSellQuantity - request.quantity());
            }
            reservedNotional = Models.money(reservedNotional.subtract(request.notional()).max(BigDecimal.ZERO));

            BigDecimal realizedDelta = request.side() == Side.BUY
                    ? applyBuy(request.quantity(), request.price())
                    : applySell(request.quantity(), request.price());
            realizedPnl = Models.money(realizedPnl.add(realizedDelta));
            return realizedDelta;
        }

        private BigDecimal applyBuy(long quantity, BigDecimal price) {
            if (netQuantity >= 0) {
                BigDecimal currentCost = averageCost.multiply(BigDecimal.valueOf(netQuantity));
                BigDecimal addedCost = price.multiply(BigDecimal.valueOf(quantity));
                netQuantity += quantity;
                averageCost = netQuantity == 0
                        ? BigDecimal.ZERO
                        : Models.money(currentCost.add(addedCost).divide(BigDecimal.valueOf(netQuantity), 4, RoundingMode.HALF_UP));
                return BigDecimal.ZERO;
            }

            long closingQuantity = Math.min(quantity, Math.abs(netQuantity));
            BigDecimal realized = averageCost.subtract(price).multiply(BigDecimal.valueOf(closingQuantity));
            netQuantity += quantity;
            if (netQuantity > 0) {
                averageCost = Models.money(price);
            } else if (netQuantity == 0) {
                averageCost = BigDecimal.ZERO;
            }
            return Models.money(realized);
        }

        private BigDecimal applySell(long quantity, BigDecimal price) {
            if (netQuantity <= 0) {
                BigDecimal currentCost = averageCost.multiply(BigDecimal.valueOf(Math.abs(netQuantity)));
                BigDecimal addedCost = price.multiply(BigDecimal.valueOf(quantity));
                netQuantity -= quantity;
                averageCost = netQuantity == 0
                        ? BigDecimal.ZERO
                        : Models.money(currentCost.add(addedCost).divide(BigDecimal.valueOf(Math.abs(netQuantity)), 4, RoundingMode.HALF_UP));
                return BigDecimal.ZERO;
            }

            long closingQuantity = Math.min(quantity, netQuantity);
            BigDecimal realized = price.subtract(averageCost).multiply(BigDecimal.valueOf(closingQuantity));
            netQuantity -= quantity;
            if (netQuantity < 0) {
                averageCost = Models.money(price);
            } else if (netQuantity == 0) {
                averageCost = BigDecimal.ZERO;
            }
            return Models.money(realized);
        }
    }
}
