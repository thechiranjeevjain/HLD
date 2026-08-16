# URL Shortener Interview Guide

## Two-Minute Pitch

This service converts long URLs into short codes, redirects users, tracks clicks, and uses Redis-style rate limiting to protect the create endpoint. It is a compact system-design project because it touches ID generation, redirects, abuse protection, and persistence.

## What To Emphasize

- Short-code generation must handle collisions.
- Redirect reads are latency-sensitive and frequent.
- Metadata reads and create writes are API workflows.
- Redis rate limiting protects the system from abusive owners or clients.
- Expiry makes old links safe to retire.

## Request Flow

1. Client posts an original URL and owner key.
2. Rate limiter checks whether the owner can create another link.
3. Service generates a candidate code and checks uniqueness.
4. PostgreSQL stores URL, owner, expiry, and click count.
5. Redirect route resolves the code and increments tracking data.

## Tradeoffs

| Decision             | Benefit                          | Cost                                                   |
| -------------------- | -------------------------------- | ------------------------------------------------------ |
| Generated short code | Compact links and simple routing | Collision handling is required                         |
| PostgreSQL metadata  | Durable links and click counts   | Very high redirect traffic may need caching            |
| Redis rate limiting  | Fast abuse control               | Redis outage needs a fail-open or fail-closed decision |
| Optional expiry      | Old links can be retired         | Redirect path must check validity                      |

## FAQ

Q: Why not hash the URL directly?
A: Hashing alone can leak patterns and still collides. A generated code with uniqueness checks is explicit and easy to reason about.

Q: What is the hardest scale problem?
A: Redirect read volume. A production version would cache hot codes and separate analytics writes from the redirect response path.

Q: What would you add next?
A: custom aliases, ownership auth, async analytics, cache-aside for redirects, and abuse dashboards.
