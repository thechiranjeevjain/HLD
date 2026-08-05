# Pre-Trade Risk Engine Demo

Runnable single-service demo for explaining a venue-neutral pre-trade risk engine.

## What It Shows

- FIX `NewOrderSingle` parsing into an internal order model.
- In-memory account, open-order, position, and market-data state.
- Sub-millisecond risk check pipeline measured in microseconds.
- Atomic check plus reservation under an account lock to prevent races.
- Firm, account, symbol, and account-symbol kill switches.
- Circuit breaker fail-closed behavior.
- Immutable audit events for every state-changing decision.
- Real-time P&L from fills and market price updates.

## Run Locally

```powershell
mvn -pl pretrade-risk-engine spring-boot:run
```

Open:

```text
http://localhost:8090
```

## Useful API Calls

Submit JSON:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8090/api/orders `
  -ContentType 'application/json' `
  -Body '{"clOrdId":"JSON-1001","account":"ACCT-DEMO","symbol":"MSFT","side":"BUY","quantity":100,"price":410.25,"autoFill":false}'
```

Submit FIX:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8090/api/fix/orders `
  -ContentType 'application/json' `
  -Body '{"message":"8=FIX.4.4|35=D|11=FIX-1001|1=ACCT-DEMO|55=MSFT|54=1|38=100|40=2|44=410.25|10=000|"}'
```

Run the race-condition demo:

```powershell
Invoke-RestMethod -Method Post http://localhost:8090/api/scenarios/race
```

Run the production-failure drill:

```powershell
Invoke-RestMethod -Method Post http://localhost:8090/api/scenarios/failure
```

## Interview Talk Track

Use the dashboard from left to right:

1. Submit a normal FIX order and show the parser, check timings, in-memory reservation, and audit event.
2. Run the race scenario. Explain that both orders independently look valid, but account-level locking makes check plus reserve atomic, so only one order consumes the available limit.
3. Turn on the account kill switch. Explain that kill switches sit at the front of the hot path because they must reject immediately.
4. Run the failure drill. Explain fail-closed behavior when market data sequence gaps or dependency health makes risk state unsafe.
5. Run the P&L scenario. Explain how fills update position state and market ticks update unrealized P&L.
