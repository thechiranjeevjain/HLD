package org.chijai;



/*
================================================================================
1. FILE HEADER (PHILOSOPHY + GOAL)
================================================================================

This file implements a deliberately small, blocking HTTP web server using
only standard Java sockets and threads.

This is NOT a production server.
This is NOT optimized for throughput.

This IS a forging artifact.

Goal:
- Train flow-of-control realism.
- Make blocking, time, backpressure, and failure visible.
- Force reasoning about ordering, resource limits, and crash points.

Core Lesson:
A web server is not code that handles requests.
It is a resource manager under adversarial timing.

Metaphor (only once):
Imagine a single checkout counter in a toy store.
Some kids pay fast, some count coins slowly, some walk away mid-payment.
The problem is not the toys — it is managing the line.

================================================================================
2. HOW TO READ THIS FILE
================================================================================

Read top to bottom once.

Then re-read only:
- SYSTEM INVARIANTS
- WRITE PATH comments
- KNOWLEDGE INDEX

Finally, trace one request end-to-end with a pencil:
accept → read → route → write → close.

Ignore elegance.
Observe ordering and failure.

================================================================================
3. SYSTEM INVARIANTS (SINGLE SOURCE OF TRUTH)
================================================================================

Invariant 1: Bounded Resources
- Threads are finite.
- Sockets (file descriptors) are finite.
- Memory is finite.
The system must fail by rejection, not by collapse.

Invariant 2: Visibility Before Acknowledgment
- A response is written only after a request is fully read and parsed.
- Partial requests never produce partial responses.

Invariant 3: Ordering Per Connection
- For a single socket: read → process → write is strictly ordered.
- No interleaving of responses on the same connection.

Invariant 4: Failure Containment
- One broken client must not affect others.
- All failures are isolated to a single connection-handling thread.

Invariant 5: Slow Clients Are Dangerous
- Reads and writes can block indefinitely.
- Timeouts and limits are mandatory to reclaim resources.

Non-Invariants (Explicitly NOT Guaranteed):
- At-least-once or exactly-once delivery
- Fair scheduling across clients
- Bounded request latency
- Response completion under slow readers
- Idempotency across crashes

Concurrency Boundary:
- Each connection is handled by exactly one thread.
- No mutable state is shared across threads.
- Memory visibility relies on thread start semantics.
- No synchronization primitives are required.

Every major code decision below exists to protect one or more invariants.

================================================================================
4. CORE IMPLEMENTATION
================================================================================
*/

import java.io.*;
        import java.net.*;
        import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class WebServer {

    /*
    ----------------------------------------------------------------
    CORE ENGINE / SERVICE
    ----------------------------------------------------------------
    */
    static class ServerEngine {

        private final int port;
        private final int maxThreads;
        private final int socketReadTimeoutMs;

        // Bounded pool enforces backpressure (Invariant 1)
        private final ExecutorService workerPool;

        ServerEngine(int port, int maxThreads, int socketReadTimeoutMs) {
            this.port = port;
            this.maxThreads = maxThreads;
            this.socketReadTimeoutMs = socketReadTimeoutMs;

            this.workerPool = new ThreadPoolExecutor(
                    maxThreads,
                    maxThreads,
                    0L,
                    TimeUnit.MILLISECONDS,
                    // Bounded queue = explicit admission control
                    new ArrayBlockingQueue<>(maxThreads),
                    new ThreadPoolExecutor.AbortPolicy()
            );
        }

        /*
        ----------------------------------------------------------------
        ACCEPT LOOP
        ----------------------------------------------------------------

        Blocking accept is intentional.
        It exposes head-of-line blocking and OS scheduling reality.
        */
        void start() throws IOException {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server listening on port " + port);

            while (true) {
                Socket client = null;
                try {
                    client = serverSocket.accept(); // BLOCKING
                    configureClientSocket(client);

                    Socket finalClient = client;
                    workerPool.execute(() -> handleConnection(finalClient));

                } catch (RejectedExecutionException ree) {
                    // Overload path: MUST close socket or FDs leak
                    // Protects Invariant 1 (bounded file descriptors)
                    if (client != null) {
                        try {
                            client.close();
                        } catch (IOException ignored) {}
                    }
                    System.err.println("Overloaded: rejecting connection");

                }
            }
        }

        private void configureClientSocket(Socket client) throws SocketException {
            // Protects Invariant 5 (slow readers)
            client.setSoTimeout(socketReadTimeoutMs);
        }

        /*
        ----------------------------------------------------------------
        PER-CONNECTION HANDLER
        ----------------------------------------------------------------

        STRICT ORDER:
        1. Read fully
        2. Parse
        3. Route
        4. Write response
        5. Close

        Reordering breaks invariants.
        */
        private void handleConnection(Socket client) {
            try (
                    InputStream in = client.getInputStream();
                    OutputStream out = client.getOutputStream()
            ) {
                // READ PATH
                HttpRequest request = readRequest(in);

                if (request == null) {
                    writeResponse(out, HttpResponse.badRequest());
                    return;
                }

                // ROUTING
                HttpResponse response = route(request);

                // WRITE PATH
                writeResponse(out, response);

            } catch (SocketTimeoutException ste) {
                // Read timed out → slow client
                // Resource reclaimed, failure isolated

            } catch (IOException ioe) {
                // Partial read, broken pipe, client vanished
                // Failure contained to this thread

            } finally {
                cleanup(client);
            }
        }

        /*
        ----------------------------------------------------------------
        READ PATH
        ----------------------------------------------------------------

        Manual byte reading exposes:
        - partial reads
        - blocking
        - framing boundaries
        */
        private HttpRequest readRequest(InputStream in) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] temp = new byte[1024];

            /*
            Crash timeline:
            - Client sends half headers
            - Server blocks here
            - SO_TIMEOUT fires
            - Connection dropped safely
            */
            while (true) {
                int read = in.read(temp); // BLOCKING
                if (read == -1) break;

                buffer.write(temp, 0, read);

                if (endsWithDoubleCRLF(buffer)) break;

                // Header bombing protection
                if (buffer.size() > 8192) return null;
            }

            String raw = buffer.toString(StandardCharsets.UTF_8);
            return HttpRequest.parse(raw);
        }

        private boolean endsWithDoubleCRLF(ByteArrayOutputStream buffer) {
            byte[] b = buffer.toByteArray();
            int len = b.length;
            return len >= 4 &&
                    b[len - 4] == '\r' &&
                    b[len - 3] == '\n' &&
                    b[len - 2] == '\r' &&
                    b[len - 1] == '\n';
        }

        /*
        ----------------------------------------------------------------
        ROUTING
        ----------------------------------------------------------------
        */
        private HttpResponse route(HttpRequest request) {
            if ("/".equals(request.path)) {
                return HttpResponse.ok("Hello from MinimalBlockingWebServer\n");
            }

            if ("/slow".equals(request.path)) {
                // Demonstrates head-of-line blocking
                sleep(3000);
                return HttpResponse.ok("Slow response finished\n");
            }

            return HttpResponse.notFound();
        }

        /*
        ----------------------------------------------------------------
        WRITE PATH
        ----------------------------------------------------------------

        Order:
        - headers
        - body
        - flush (acknowledgment)
        */
        private void writeResponse(OutputStream out, HttpResponse response) throws IOException {
            out.write(response.toBytes());
            out.flush();

            /*
            WRITE-SIDE FAILURE NOTE:

            There is NO write timeout.

            Failure scenario:
            - Client stops reading
            - OS send buffer fills
            - write()/flush() blocks indefinitely
            - Worker thread is lost until client disappears

            This is deliberate.
            It exposes asymmetric timeout risk:
            - Reads protected
            - Writes vulnerable
            */
        }

        private void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignored) {}
        }

        private void cleanup(Socket client) {
            try {
                client.close();
            } catch (IOException ignored) {}
        }
    }

    /*
    ----------------------------------------------------------------
    SUPPORTING COMPONENTS
    ----------------------------------------------------------------
    */

    static class HttpRequest {
        final String method;
        final String path;

        HttpRequest(String method, String path) {
            this.method = method;
            this.path = path;
        }

        static HttpRequest parse(String raw) {
            try {
                String[] lines = raw.split("\r\n");
                String[] parts = lines[0].split(" ");
                return new HttpRequest(parts[0], parts[1]);
            } catch (Exception e) {
                return null;
            }
        }
    }

    static class HttpResponse {
        final int status;
        final String body;

        HttpResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }

        static HttpResponse ok(String body) {
            return new HttpResponse(200, body);
        }

        static HttpResponse notFound() {
            return new HttpResponse(404, "Not Found\n");
        }

        static HttpResponse badRequest() {
            return new HttpResponse(400, "Bad Request\n");
        }

        byte[] toBytes() {
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            String header =
                    "HTTP/1.1 " + status + " \r\n" +
                            "Content-Length: " + bodyBytes.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n";
            byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);

            byte[] result = new byte[headerBytes.length + bodyBytes.length];
            System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
            System.arraycopy(bodyBytes, 0, result, headerBytes.length, bodyBytes.length);
            return result;
        }
    }

    /*
    =================================================================================
    6. DEMO / SMOKE TEST
    =================================================================================
    */
    public static void main(String[] args) throws Exception {
        int port = 8080;
        int maxThreads = 4;
        int timeoutMs = 5000;

        ServerEngine server = new ServerEngine(port, maxThreads, timeoutMs);
        server.start();
    }

    /*
    =================================================================================
    7. KNOWLEDGE INDEX (CONTROLLED COMPRESSION)
    =================================================================================

    Mental Models:
    - Requests are streams over time, not function calls.
    - Blocking hides until load or slowness reveals it.
    - Backpressure is survival, not fairness.

    Failure Timelines (Concrete):
    1) accept() succeeds → executor rejects
       - Risk: socket leak
       - Protection: explicit close

    2) read() blocks → client stalls
       - Protection: SO_TIMEOUT
       - Outcome: thread reclaimed

    3) write() blocks → client stops reading
       - No protection here
       - Outcome: thread starvation

    4) crash after flush()
       - Client may see full response
       - Server state lost
       - Idempotency not guaranteed

    Trade-offs:
    - Thread-per-connection
      + Simple reasoning
      - Head-of-line blocking

    - Bounded pool
      + Predictable failure
      - Requests dropped

    - Manual parsing
      + Partial reads visible
      - More code, fewer comforts

    Backpressure ≠ Fairness:
    - Limits total load
    - Does not ensure equal progress
    - Slow handlers can monopolize threads

    OS Mapping:
    - Socket = file descriptor
    - Thread = kernel-scheduled entity
    - Blocking I/O = kernel wait queue
    - Timeout = kernel wakeup + exception

    Interview Anchors:
    - Why no ConcurrentHashMap?
      No shared mutable state exists.

    - Weakest point?
      Write-side blocking under slow readers.

    - First production change?
      Non-blocking I/O with explicit state machines.
    */
}
