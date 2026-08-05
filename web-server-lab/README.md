# Web Server Lab

A small Java 17 lab that implements a blocking HTTP server using standard sockets and a bounded thread pool. It is designed to expose request framing, blocking I/O, slow clients, backpressure, and failure containment.

## What It Shows

- Blocking `ServerSocket.accept()`.
- One worker thread per connection.
- Bounded executor queue for overload behavior.
- Read timeout for slow clients.
- Manual HTTP request parsing.
- Basic routing for `/` and `/slow`.

## Run

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\web-server-lab
mvn clean package
java -jar target/web-server-lab-0.1.0-SNAPSHOT.jar
```

The server listens on `http://localhost:8080`.

## Smoke Test

```powershell
Invoke-WebRequest http://localhost:8080/
Invoke-WebRequest http://localhost:8080/slow
Invoke-WebRequest http://localhost:8080/missing
```

## Learning Docs

- [Interview Guide](docs/INTERVIEW_GUIDE.md)
- [Diagrams](docs/DIAGRAMS.md)
- [Demo Script](docs/DEMO_SCRIPT.md)
