# cli

The `cli` module implements the `mc` operator workflow.

Examples:

```powershell
java -cp "cli\target\exchange-lite-cli-0.1.0.jar;common\target\exchange-lite-common-0.1.0.jar" io.exchangelite.cli.MarketConsole stats
java -cp "cli\target\exchange-lite-cli-0.1.0.jar;common\target\exchange-lite-common-0.1.0.jar" io.exchangelite.cli.MarketConsole heap
```

Set `EXCHANGE_SIDECAR_URL` to point the CLI at another sidecar:

```powershell
$env:EXCHANGE_SIDECAR_URL="http://127.0.0.1:8080"
```

The CLI intentionally talks only to the sidecar. Direct CLI access to the engine would bypass the control plane and make production authorization, audit, and rate limiting harder.
