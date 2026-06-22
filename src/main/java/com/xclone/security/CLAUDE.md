# Security — JWT Infrastructure

This package contains the security infrastructure (filter, config, token provider). The
feature-level auth documentation — endpoints, token strategy, validation, OpenAPI docs, and
testing — lives in `../auth/CLAUDE.md`.

## Key classes

- `SecurityConfig` — `@Configuration`; defines `SecurityFilterChain` with `permitAll()` on
  `/api/auth/**`, `/graphql`, `/graphiql` and authenticated everything else.
- `JwtAuthenticationFilter` — validates Bearer tokens on protected routes; UUID conversion
  happens once here.
- `JwtTokenProvider` — HMAC-SHA256 JWT creation and validation.
- `JwtConfig` / `JwtProperties` — externalized JWT configuration.
- `CustomUserDetails` — `getId()` returns UUID directly.
