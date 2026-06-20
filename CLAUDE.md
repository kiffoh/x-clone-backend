# X-Clone Backend

Spring Boot backend for an X/Twitter-style social platform. This is the developer's first
Spring Boot project (transitioning from Node.js/Express). The approach throughout is
learning-oriented — understanding *why* decisions are made, not just what to implement.

## Learning Approach

I am learning Spring Boot and want to be guided toward answers rather than given them
directly. Please ask guiding questions, point to the right concepts to research, and only
provide direct answers when I explicitly ask for them or when a direct answer is clearly the
most helpful response (e.g. a factual definition or a specific syntax question with no
learning value in withholding it). If I suggest the correct approach myself, guide me to
implement it rather than providing it directly.

## Tech Stack

- Spring Boot 3.5.10
- Spring Security + JWT (jjwt 0.13.0)
- Spring Data JPA + PostgreSQL 16 (Docker)
- Spring Data Redis 7 (Docker)
- Spring for GraphQL
- springdoc-openapi 2.8.16 (Swagger UI)
- JUnit 5 + Mockito + AssertJ + Testcontainers
- Lombok
- graphql-java-extended-scalars 22.0

## Architecture

- **Vertical slice** per feature. Explicit `@Controller → @Service → @Repository` layering —
  `@GraphQlRepository` is never used (every query needs auth, status filtering, or DTO
  projection; none are pass-through lookups).
- **JPA entities never appear in DTOs or resolver return types.** Each entity exposes a
  `toXProfile()` method projecting to an immutable Java `record` (`UserProfile`, `PostProfile`,
  `NotificationProfile`, …) — the public-facing type.
- **Soft delete** via a shared `Status` enum (`ACTIVE` / `HIDDEN` / `DELETED`) in
  `common/enums/Status.java`, used wherever the same states apply. Follows are the exception —
  hard-deleted on unfollow.
- `Instant` is kept on entities for storage; converted to `OffsetDateTime` via
  `atOffset(ZoneOffset.UTC)` in the `toXProfile()` mapper for GraphQL serialisation.

## Repo-wide conventions

- **Method-name prefixes:** repository query methods use `find` (`findShareCounts`,
  `findFirstPageOfFeed`); service methods use `get` (`getShareCounts`, `getRepostUsers`). The
  two conventions are kept separate and consistent within their layers.
- **Constraint names:** every constraint-name constant uses the `{domain}_constraint_{name}`
  convention (e.g. `post_constraint_author_id_fk`, `follow_constraint_self_follow`). Lowercase,
  to match PostgreSQL's reported names. Full rationale: `jpa-entity-conventions` skill.
- **GraphQL schema comments:** `"""` descriptions are client-facing API docs; `#` comments are
  dev notes / implementation reminders. Never put a dev note in a `"""` block.
- **Tests use AssertJ** as the default assertion library. In GraphQL tests, assert
  `"errors": null` explicitly (matching is lenient by default).
- **`@Enumerated(EnumType.STRING)`** on every enum field — never rely on the `ORDINAL` default.
- **SpotBugs null-safety:** when a chain of calls returns nullable values
  (`getRootCause()` → `getServerErrorMessage()` → `getConstraint()`), store each result in a
  local and null-check before the next call; fall through to default behaviour on null. This
  satisfies `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` without suppression.
- **Type-name collisions:** when a project type and a framework type share a simple name (e.g.
  `FieldError`), reference the less-used one by fully qualified name inline rather than importing.
- **Javadoc:** production-quality Javadoc across the codebase; test omissions are documented
  with a `{@link}` to the test that covers the case elsewhere. One-line self-describing
  delegators may omit Javadoc deliberately.
- **Query resolvers return `null` for unavailable resources** (not-found *and* inactive);
  exceptions are reserved for mutation contexts where the caller must act on the distinction.

## Index — where the detail lives

Each feature has a nested `CLAUDE.md` in its package (auto-loads when you edit those files).
Cross-cutting reference lives in skills (invoke by intent). Project TODOs live in `STATUS.md`.

| Area | What it is | Where |
|---|---|---|
| Auth | JWT + refresh-token REST auth, OpenAPI docs | `src/main/java/com/xclone/auth/CLAUDE.md` (+ `security/`) |
| User | User GraphQL slice (queries, mutations, profile, search/suggested) | `user/CLAUDE.md` |
| Follow | Follow/unfollow, followers/following pagination | `follow/CLAUDE.md` |
| Post | Post entity, feed, getPost, FK-by-UUID self-refs | `post/CLAUDE.md` |
| Reply | Replies, reply-thread view (recursive CTE), createReply | `reply/CLAUDE.md` |
| Share | Reposts + quotes ("share" umbrella), share counts | `share/CLAUDE.md` |
| Notification | Notifications + actors, read side + triggers (in progress) | `notification/CLAUDE.md` |
| Like | Likes, like counts, NotPostAuthor authorization | `like/CLAUDE.md` |
| Exception | REST + GraphQL error handling, error mapper | `exception/CLAUDE.md` |
| Validation | `@ValidHandle` / `@ValidPassword` / `ValidationConstants` | `validation/CLAUDE.md` |
| Pagination | Cursor/keyset engine, `Connection` types, Slice-vs-Page | `pagination` skill |
| GraphQL conventions | Schema style, `@SchemaMapping` vs `@BatchMapping`, error model | `graphql-conventions` skill |
| JPA conventions | FK-by-UUID, constraint naming, enums, surrogate PKs | `jpa-entity-conventions` skill |
| Testing | Test tiers, base classes, fixtures, GraphQlTester, build/CI | `writing-tests` skill |
| Project status | Next steps, in-progress work, deferred items | `STATUS.md` |
