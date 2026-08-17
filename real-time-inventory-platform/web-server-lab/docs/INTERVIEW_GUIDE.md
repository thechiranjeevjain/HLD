# Web Server Lab Interview Guide

## Two-Minute Pitch

This lab implements a minimal blocking HTTP server without Spring. It shows what frameworks usually hide: sockets, blocking reads, request framing, worker pools, overload rejection, and connection cleanup.

## What To Emphasize

- A web server is a resource manager under timing pressure.
- Accepting a connection is separate from successfully handling it.
- Slow reads are protected with socket timeouts.
- Write-side blocking is a known weakness in this version.
- Bounded thread pools create predictable overload behavior.

## Tradeoffs

| Decision | Benefit | Cost |
| --- | --- | --- |
| Blocking I/O | Easy to understand | Threads can be exhausted |
| Bounded worker pool | Avoids unlimited thread growth | Excess connections are rejected |
| Manual parsing | Makes framing visible | Not HTTP-complete |
| No shared state | Simple concurrency model | Only supports simple routes |

## FAQ

Q: Why not use Spring Boot?
A: The point is to see the lower-level mechanics that Spring hides.

Q: What breaks under slow clients?
A: Read timeout protects reads, but slow readers can still block writes and occupy worker threads.

Q: What would you add next?
A: non-blocking I/O, write timeouts, HTTP keep-alive, request body parsing, TLS, and structured access logs.
