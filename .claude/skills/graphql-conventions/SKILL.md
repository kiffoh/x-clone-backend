---
name: graphql-conventions
description: GraphQL schema conventions, resolver mapping rules (@SchemaMapping vs @BatchMapping), error model (errors-as-data vs protocol errors), and schema documentation style for the X-Clone Spring Boot backend — covers schema.graphqls, post.graphqls, user.graphqls, notification.graphqls, common.graphqls, GraphQlExceptionHandler, and GraphQlErrorMapper.
---

# GraphQL Conventions

## Schema documentation style

- `"""` descriptions are **client-facing API docs** — never put dev notes here.
- `#` comments are **dev notes / implementation reminders** — never expose to clients.
- `"Authentication: Required."` added to all protected query endpoints, not just those that
  use `@AuthenticationPrincipal` — the client needs to know whether a Bearer token is required
  regardless of whether the server uses the identity internally.
- `DateTime` scalar documented with expected ISO-8601 format.
- Input type constraints documented at the type level (e.g. `UpdateUserInput` at-least-one-field).

## Resolver mapping rules

See [references/resolver-mapping.md](references/resolver-mapping.md) for full decision
rationale with examples.

**Quick reference:**

| Pattern | Use | Why |
|---|---|---|
| `@SchemaMapping` | `parent` on Post | Only resolves on single-post views (replies filtered from feed); no N+1 risk. |
| `@BatchMapping` | `sharedPost`, `replyCount`, `shareCount`, `sharedByMe`, `isFollowing`, `actors`, `actorCount`, `post` (notification) | Multiple items in a list/feed — batching avoids N+1. |
| `@BatchMapping` (not JPA eager) | all batch fields | GraphQL is demand-driven; eager fetching at JPA level pays for resolution the client didn't ask for. |

- `@BatchMapping` returns only entries with non-null results — missing keys resolve to `null`
  automatically by the framework. Same pattern across `sharedPost`, `isFollowing`, `post`
  (notification).
- `@BatchMapping` for `sharedByMe` / `isFollowing` retrieves auth context from
  `SecurityContextHolder` directly.

## Error model

See [references/error-model.md](references/error-model.md) for full decision detail.

**Quick reference:**

- **Mutations:** errors as data in the response payload (`XResponse.errors: [FieldError]`).
  Known failure modes (duplicate, not-found, forbidden) are caught in the controller and
  mapped via `GraphQlErrorMapper` to payload errors with HTTP-style codes ("200", "400",
  "403", "404", "409").
- **Queries:** plain domain types with no errors field. Validation failures and not-found
  cases bubble as protocol-level GraphQL errors via `GraphQlExceptionHandler`.
- **`GraphQlExceptionHandler`** is a last resort — for unexpected exceptions on any resolver
  and for expected exceptions on query resolvers; must never leak exception messages.
- Two separate exception handlers are correct — `GlobalExceptionHandler` (REST) and
  `GraphQlExceptionHandler` (GraphQL) are completely independent execution paths.

## Mutation argument conventions

- **No input wrapper for single-argument mutations:** `createRepost(sharedPostId: ID!)` uses
  a bare `ID!` argument, not a `CreateRepostInput`. A single field does not justify a wrapper.
- **Mutations that change server state are always under `Mutation`:** `readNotification` is a
  mutation, not a query, even though it's a simple operation. Queries must be side-effect-free.

## Naming conventions

- **"Share" as umbrella term** for reposts + quotes combined. `shareCount` / `sharedByMe` cover
  both; `reposts` is pure reposts only; `quotes` is quotes only.
- **`reposts` returns `UserConnection`** (not `PostConnection`) — pure reposts have no
  `messageContent`; the "Reposts" tab shows users who reposted, not posts.
- **`sharedPostId` over `quotedPostId`** — both reposts and quotes use this field.
- **`sharedPost` over `originalPost`** — accurate at every level in quote chains.
- **`sharedPostId` exposed in schema** — lets client distinguish "regular post"
  (`sharedPostId` null) from "share whose original was deleted" (`sharedPostId` non-null,
  `sharedPost` null).

## Scalar configuration

- `ExtendedScalars.DateTime` registered via `RuntimeWiringConfigurer` in `GraphQlConfig`.
- `@Import(GraphQlConfig.class)` required in `@GraphQlTest` slice tests to register the scalar.
