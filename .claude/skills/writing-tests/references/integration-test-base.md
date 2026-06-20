# Integration Test Base Classes

## Hierarchy

```
BaseIntegrationTest (Testcontainers: PostgreSQL + Redis)
├── BaseGraphQlIntegrationTest (@AutoConfigureHttpGraphQlTester)
│   ├── UserIT, FollowIT, PostIT, ReplyIT, ShareIT, LikeIT, NotificationIT
│   └── ValidationIT, SecurityIT
└── BaseAuthIntegrationTest (TestRestTemplate)
    └── AuthenticationIT
```

## BaseIntegrationTest

- Provides PostgreSQL and Redis Testcontainers.
- **No `@Testcontainers` / `@Container` annotations** — these tie container lifecycle to the
  test class, causing the PostgreSQL container to stop after the first IT class finishes while
  the cached Spring context still holds a datasource pointing at the now-dead port.
- Replaced with manual `static { postgreSQLContainer.start(); }` initialiser block — runs once
  when the class is loaded by the JVM; container stays alive for the entire test suite.
- `@ServiceConnection` retained — still handles wiring datasource properties from the running
  container.
- Testcontainers' built-in Ryuk sidecar automatically cleans up containers when the JVM exits
  — no manual `stop()` needed.

## BaseGraphQlIntegrationTest

- Extends `BaseIntegrationTest`.
- Contains **all repositories** needed across GraphQL IT classes — single place to add new
  repositories as entity types grow.
- `@BeforeEach cleanupDBs()` — deletes from all tables in FK-safe order (child tables before
  parent tables); runs before every test across all subclasses.
- Superclass `@BeforeEach` runs **before** subclass `@BeforeEach` — subclasses start with a
  guaranteed clean database before their own setup.
- Eliminates per-IT-class cleanup methods — previously each IT class maintained its own
  `cleanupDBs()` that only knew about its own repositories, causing FK violations when test
  execution order differed between IntelliJ and Maven Failsafe.
- Notification repositories (`NotificationActorRepository`, `NotificationRepository`) added to
  `wipeDBs()` — deleted before likes, posts, follows, and users to respect FK ordering.

## BaseAuthIntegrationTest

- Extends `BaseIntegrationTest`.
- Separate base class for REST/auth integration tests that use `TestRestTemplate` instead of
  `HttpGraphQlTester`.

## Why two intermediate classes

- Auth ITs use `TestRestTemplate`; GraphQL ITs use `HttpGraphQlTester`.
- Shared Testcontainers infrastructure lives in the common parent; protocol-specific setup
  lives in the intermediate classes.

## Test execution order

- IntelliJ runs tests via JUnit directly; Maven Failsafe runs `*IT` classes during the
  `integration-test` lifecycle phase. The two tools may execute test classes in different
  orders.
- Any cleanup strategy that depends on a specific execution order will fail in one environment
  or the other.
- Centralised FK-safe cleanup in a base class `@BeforeEach` eliminates order dependence.
