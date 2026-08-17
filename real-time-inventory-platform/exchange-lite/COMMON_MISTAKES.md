# Common Mistakes

- Putting HTTP directly in the data plane.
- Using floating point values for money.
- Matching at the aggressive order price instead of the resting price.
- Forgetting FIFO within a price level.
- Allowing market orders to rest on the book.
- Treating sidecar health as identical to engine health.
- Bypassing IPC from the CLI.
- Assuming a `HashMap` can expose best bid or ask without an auxiliary structure.
- Calling a deterministic unit test a load test.
- Claiming Java 21 readiness without running the Java 21 toolchain.
