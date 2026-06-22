---
name: writing-tests
description: Test patterns, tiers, and conventions for the X-Clone Spring Boot backend — covers JUnit 5, Mockito, AssertJ, @GraphQlTest slice tests, @SpringBootTest integration tests with Testcontainers, GraphQlTester/HttpGraphQlTester gotchas, BaseIntegrationTest hierarchy, test fixtures and helpers, ValidationIT structure, Maven Failsafe/JaCoCo/SpotBugs build config, and FK-aware cleanup patterns.
---

# Test Patterns & Conventions

## Test tiers

1. **Unit tests** (`@ExtendWith(MockitoExtension.class)`) — service logic with mocked
   dependencies. Exception handlers and error mappers are instantiated directly (no Spring
   context needed — `@ControllerAdvice` only registers at runtime).
2. **Controller slice tests** (`@GraphQlTest` / `@WebMvcTest`) — lightweight GraphQL or MVC
   slice. `@MockitoBean` for every service dependency. `@Import(GraphQlConfig.class)` required
   to register `DateTime` scalar.
3. **Integration tests** (`@SpringBootTest` + Testcontainers) — full HTTP requests via
   `HttpGraphQlTester` or `TestRestTemplate`.

## Naming & file conventions

- Unit/slice tests: `*Test.java` — picked up by Maven **Surefire**.
- Integration tests: `*IT.java` — picked up by Maven **Failsafe** (must be in `pom.xml`).
- Fixtures: `support/fixtures/*Fixtures.java` — entity builders with manual field control.
- Helpers: `support/helpers/*Helpers.java` — `@TestComponent` beans or `public static`
  methods for seeding and cleanup.

## Key patterns

- **Authentication:** `@WithMockCustomUser` annotation backed by
  `WithSecurityContextFactory<WithMockCustomUser>`. `AuthHelpers` is a `@TestComponent`
  injected via `@Import`.
- **Unauthenticated rejection:** tested once globally in `SecurityIT` — not repeated per
  feature. Omission documented with Javadoc `{@link SecurityIT}`.
- **Feature IT keeps one validation smoke test** to confirm wiring; exhaustive validation
  coverage lives in `ValidationIT`.
- **Test helper extraction:** inline patterns that appear in ≥2 IT classes are extracted to
  `*Helpers` as `public static` methods on first reuse.
- **`deletePostTests` made `public`** so that `{@link}` references from other IT packages
  resolve at compile time.

## FK-aware cleanup

See [references/test-cleanup.md](references/test-cleanup.md).

## GraphQlTester patterns

See [references/graphql-tester-patterns.md](references/graphql-tester-patterns.md).

## Integration test base classes

See [references/integration-test-base.md](references/integration-test-base.md).

## ValidationIT structure

See [references/validation-it.md](references/validation-it.md).

## Build & CI

See [references/build-ci.md](references/build-ci.md).
