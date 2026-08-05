# ExchangeLite Cheatsheet

- Data plane: binary TCP, no HTTP.
- Control plane: sidecar REST plus IPC.
- IPC default: localhost TCP on port `9191`.
- Data TCP default: port `9090`.
- Sidecar default: port `8080`.
- Matching rule: price-time priority.
- Buy crosses if buy price is greater than or equal to best ask.
- Sell crosses if sell price is less than or equal to best bid.
- Resting order sets trade price.
- Market residual is cancelled, not rested.
- Risk happens before matching.
- Sidecar route failure to IPC returns `502`.
- Best first interview answer: this project separates operational control from latency-sensitive matching.
