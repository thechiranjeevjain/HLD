# Authentication Service Interview Guide

## Two-Minute Pitch

This service demonstrates the authentication boundary for a backend system. Users can register, log in, receive a signed JWT, and call protected endpoints. Admin-only endpoints show role-based authorization. A mock OAuth2 endpoint keeps the identity-provider flow visible without requiring a real external provider during local demos.

## What To Emphasize

- Passwords are hashed with BCrypt before persistence.
- JWTs keep read-side authentication stateless after login.
- Spring Security enforces route-level access control before controller logic runs.
- Roles separate ordinary user access from admin operations.
- Flyway makes the database schema reproducible.

## Request Flow

1. Client posts registration or login details.
2. `AuthController` validates the request body.
3. `AuthService` checks uniqueness or credentials.
4. Passwords are hashed or verified with BCrypt.
5. `JwtService` signs a token with role claims.
6. Later requests pass the bearer token through `JwtAuthenticationFilter`.

## Tradeoffs

| Decision              | Benefit                                   | Cost                                  |
| --------------------- | ----------------------------------------- | ------------------------------------- |
| JWT bearer tokens     | Stateless API authentication              | Token revocation needs extra design   |
| BCrypt                | Slow password hashing resists brute force | Login is intentionally CPU heavier    |
| Mock OAuth2 endpoint  | Easy local demo of federated identity     | Not a real provider integration       |
| Database-backed users | Durable accounts and admin queries        | Requires migrations and DB operations |

## Failure Cases To Discuss

- Duplicate registration returns a controlled conflict instead of corrupting users.
- Bad credentials do not reveal whether email or password was wrong.
- Expired or malformed JWTs are rejected before business logic.
- Missing admin role blocks `/api/admin/users`.

## FAQ

Q: Why not store sessions in Redis?
A: JWTs are simpler for this learning service. A production design could add refresh tokens or token revocation state.

Q: Where is authorization enforced?
A: At the Spring Security filter chain and method/route rules, before protected controller logic is reached.

Q: What would you add for production?
A: Refresh-token rotation, rate limiting, account lockout, real OAuth2 providers, audit logs, secrets management, and TLS.
