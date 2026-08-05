# Distributed Database Demo Script

## Build

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\distributed-database
mvn clean package
```

## Start Three Nodes

Open three PowerShell terminals.

Terminal 1:

```powershell
java -jar target/distributed-database-0.1.0-SNAPSHOT.jar --node-id node1 --bind-host 127.0.0.1 --port 9101 --peers node1=127.0.0.1:9101,node2=127.0.0.1:9102,node3=127.0.0.1:9103 --data-dir data/node1
```

Terminal 2:

```powershell
java -jar target/distributed-database-0.1.0-SNAPSHOT.jar --node-id node2 --bind-host 127.0.0.1 --port 9102 --peers node1=127.0.0.1:9101,node2=127.0.0.1:9102,node3=127.0.0.1:9103 --data-dir data/node2
```

Terminal 3:

```powershell
java -jar target/distributed-database-0.1.0-SNAPSHOT.jar --node-id node3 --bind-host 127.0.0.1 --port 9103 --peers node1=127.0.0.1:9101,node2=127.0.0.1:9102,node3=127.0.0.1:9103 --data-dir data/node3
```

## Commands

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

## Failure Drill

Stop `node3`, write a key through `node1`, restart `node3`, then run:

```powershell
Send-DbCommand 9103 "RECOVER"
Send-DbCommand 9103 "GET invoice:7"
```

## Interview Close

Say: this is not production consensus. It is a learning database that makes leader forwarding, quorum, hashing, WAL, and repair concrete.
