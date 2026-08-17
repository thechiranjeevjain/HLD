# ExchangeLite Demo Script

## Build

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\exchange-lite
mvn test
mvn package
```

## Start Local Runtime

Terminal 1:

```powershell
java -cp "engine\target\exchange-lite-engine-0.1.0.jar;common\target\exchange-lite-common-0.1.0.jar" io.exchangelite.engine.app.EngineApplication
```

Terminal 2:

```powershell
java -cp "sidecar\target\exchange-lite-sidecar-0.1.0.jar;common\target\exchange-lite-common-0.1.0.jar" io.exchangelite.sidecar.SidecarApplication
```

Terminal 3:

```powershell
java -cp "cli\target\exchange-lite-cli-0.1.0.jar;common\target\exchange-lite-common-0.1.0.jar" io.exchangelite.cli.MarketConsole health
java -cp "cli\target\exchange-lite-cli-0.1.0.jar;common\target\exchange-lite-common-0.1.0.jar" io.exchangelite.cli.MarketConsole stats
java -cp "cli\target\exchange-lite-cli-0.1.0.jar;common\target\exchange-lite-common-0.1.0.jar" io.exchangelite.cli.MarketConsole markets
```

## Talk Track

1. Show the architecture diagram.
2. Explain why trading traffic uses the binary TCP data plane.
3. Explain why the sidecar owns HTTP management.
4. Run CLI health and stats.
5. Open `REQUEST_LIFECYCLE.md` and trace order handling.

## Interview Close

Say: the main design lesson is boundary control. Latency-sensitive matching should stay isolated from management endpoints and operational tooling.
