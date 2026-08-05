# Distributed Database

An interview-sized distributed key-value database. It deliberately avoids RocksDB, Cassandra-style breadth, external brokers, and heavyweight frameworks so the distributed systems mechanics stay visible in the code.

## What It Implements

| Phase | Included behavior |
| --- | --- |
| 1. TCP server and multiple nodes | Each node is a Java TCP server. A local cluster runs as three independent processes on ports `9101`, `9102`, and `9103`. |
| 2. Leader election | Nodes heartbeat each configured peer and elect the first live node in the configured peer list as leader. Followers forward writes to the current leader. |
| 3. Replication | The leader writes each key to its replica set. Internal replication commands apply versioned records on peer nodes. |
| 4. Consistent hashing | A SHA-256 hash ring with virtual nodes maps keys to replica sets. |
| 5. Failure recovery | Each node persists an append-only WAL and can run `RECOVER` to sync missed replica records from peers after restart. |
| 6. Read/write quorum | Defaults to replication factor `3`, read quorum `2`, and write quorum `2`. Reads choose the newest version and repair stale replicas reached during the read. |

This is a learning database, not a production database. There is no compaction, snapshotting, membership gossip, Merkle tree repair, TLS, authentication, or consensus log.

## TCP Commands

| Command | Purpose |
| --- | --- |
| `PUT <key> <value>` | Store a value through the leader and write quorum. Values may contain spaces. |
| `GET <key>` | Read from the key's replica set using read quorum. |
| `DELETE <key>` | Write a tombstone through the leader and write quorum. |
| `STATUS` | Show node id, role, leader, live peers, quorum settings, and local record count. |
| `RING` | Show configured ring members. |
| `RECOVER` | Pull missed records for this node from peers. |
| `HELP` | Show command summary. |

Responses are single-line text responses such as:

```text
OK operation=PUT key=user:42 version=1785929000123 acknowledged=3 quorum=2 replicas=node2,node1,node3
VALUE key=user:42 version=1785929000123 Alice Smith
NOT_FOUND key=user:42
ERROR write quorum failed operation=PUT key=isolated required=2 acknowledged=1 ...
```

## Verify

```powershell
mvn clean verify
```

The integration tests start three in-process TCP nodes, verify follower write forwarding, replicated reads, write-quorum failure, and peer recovery after a node misses a write.

## Run Locally With Java

Build the jar:

```powershell
mvn clean package
```

Open three PowerShell terminals from this project directory.

Terminal 1:

```powershell
java -jar target/distributed-database-0.1.0-SNAPSHOT.jar `
  --node-id node1 `
  --bind-host 127.0.0.1 `
  --port 9101 `
  --peers node1=127.0.0.1:9101,node2=127.0.0.1:9102,node3=127.0.0.1:9103 `
  --data-dir data/node1
```

Terminal 2:

```powershell
java -jar target/distributed-database-0.1.0-SNAPSHOT.jar `
  --node-id node2 `
  --bind-host 127.0.0.1 `
  --port 9102 `
  --peers node1=127.0.0.1:9101,node2=127.0.0.1:9102,node3=127.0.0.1:9103 `
  --data-dir data/node2
```

Terminal 3:

```powershell
java -jar target/distributed-database-0.1.0-SNAPSHOT.jar `
  --node-id node3 `
  --bind-host 127.0.0.1 `
  --port 9103 `
  --peers node1=127.0.0.1:9101,node2=127.0.0.1:9102,node3=127.0.0.1:9103 `
  --data-dir data/node3
```

Send commands from another PowerShell terminal:

```powershell
function Send-DbCommand {
  param([int] $Port, [string] $Command)
  $client = [System.Net.Sockets.TcpClient]::new("127.0.0.1", $Port)
  $stream = $client.GetStream()
  $writer = [System.IO.StreamWriter]::new($stream)
  $reader = [System.IO.StreamReader]::new($stream)
  $writer.AutoFlush = $true
  $writer.WriteLine($Command)
  $reader.ReadLine()
  $client.Close()
}

Send-DbCommand 9102 "STATUS"
Send-DbCommand 9102 "PUT user:42 Alice Smith"
Send-DbCommand 9103 "GET user:42"
Send-DbCommand 9101 "RING"
```

## Run With Docker Compose

```powershell
docker compose up --build
```

Then use the same `Send-DbCommand` helper against `localhost:9101`, `localhost:9102`, or `localhost:9103`.

## Failure Drill

1. Start all three nodes.
2. Stop `node3`.
3. Send a write to `node1`:

```powershell
Send-DbCommand 9101 "PUT invoice:7 paid"
```

4. Restart `node3`.
5. Trigger recovery on `node3`:

```powershell
Send-DbCommand 9103 "RECOVER"
Send-DbCommand 9103 "GET invoice:7"
```

With replication factor `3` and write quorum `2`, the write can succeed while one replica is down. The recovered node catches up from the surviving peers.
