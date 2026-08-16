# Capacity and Scaling

Assume 10,000 stores, 100,000 active SKUs, an average 5 updates/second/store, and a 10x promotion peak:

- average: 50,000 updates/second;
- peak: 500,000 updates/second;
- at roughly 300 bytes/event: 15 MB/s average and 150 MB/s peak before replication;
- one latest row per active `(sku, store)` pair, not the 1 billion theoretical Cartesian product.

Start with enough Kafka partitions for peak processing plus headroom. Partition count follows measured per-consumer throughput; it is not guessed from store count. Hot SKUs do not create a hot partition when the key includes `storeId`, though regional summary aggregation needs a second repartition stage.

## Large-stream latest state

For bounded files, hash-reduce by `(sku, storeId)`. For unbounded streams, keep one state entry per key, checkpoint it, and compact obsolete versions. A watermark is needed for event-time reporting but not for correctness when the source version is authoritative.

## Large-dataset duplicate transactions

1. Normalize account, merchant, currency, and integer amount.
2. Hash-partition by the non-time fingerprint.
3. Sort each partition by fingerprint and timestamp.
4. Scan a bounded time window and emit duplicate clusters.
5. Persist canonical transaction IDs for audit and remediation.

Bloom filters can avoid many remote lookups but cannot decide duplication because false positives exist. A durable key lookup remains authoritative.

## SLOs and alerts

- Query p99 below 100 ms in-region.
- 99.9% availability for reads and update ingestion.
- 99% of updates visible within 2 seconds under normal load.
- Alert on consumer lag age, stale-store percentage, rejected schema rate, DLQ growth, cache hit ratio, and projection conflicts.
