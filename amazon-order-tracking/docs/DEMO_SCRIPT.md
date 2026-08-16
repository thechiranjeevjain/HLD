# Five-Minute Demo

1. Run `mvn clean verify`; explain the four integration tests and what is not infrastructure-tested.
2. Run `mvn spring-boot:run` and open `http://127.0.0.1:8080`.
3. Show `ORD-1001`: two packages and one merged timeline.
4. Run `scripts\demo-events.ps1`; refresh to show near-real-time `OUT_FOR_DELIVERY`.
5. Run it again. Point out `DUPLICATE`, unchanged history, and idempotent outcome.
6. In a terminal, read as another customer and show `403`; read as support and show success:

```powershell
Invoke-WebRequest http://127.0.0.1:8080/orders/ORD-1001/tracking -Headers @{"X-Actor-Id"="other-user"} -SkipHttpErrorCheck
Invoke-RestMethod http://127.0.0.1:8080/orders/ORD-1001/tracking -Headers @{"X-Actor-Id"="agent-7";"X-Actor-Role"="SUPPORT"}
```

7. Open `docs/ARCHITECTURE.md` and bridge each executable local component to its production equivalent.

Reset the demo by restarting the app; H2 is intentionally in-memory.
