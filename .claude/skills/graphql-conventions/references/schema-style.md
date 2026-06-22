# Schema Documentation Style

## Documentation syntax

- `"""` descriptions → client-facing API docs.
- `#` comments → dev notes / implementation reminders. Never `"""` for dev notes.

## Auth annotation

`"Authentication: Required."` added to all protected query and mutation endpoints — not just
those using `@AuthenticationPrincipal`. The client needs to know whether a Bearer token is
required regardless of whether the server uses the identity internally. Applied to `getPost`,
`getReplyThread`, `feed`, `me`, `suggestedUsers`, etc.

## Type documentation

- Input type constraints documented at the type level (e.g. `UpdateUserInput` at-least-one-field
  constraint).
- `DateTime` scalar documented with expected ISO-8601 format.
- `UserResponse` description: "Result of a mutating operation on a user." — consistent with
  `PostResponse`.

## Connection types

- `totalCount` removed from all connection types — semantically misaligned with cursor
  pagination.
- Follower/following counts exposed as dedicated fields instead.

## Schema file organisation

- `schema.graphqls` — root `Query` and `Mutation` types with `extend type` per feature.
- `user.graphqls`, `post.graphqls`, `notification.graphqls`, `common.graphqls` — per-feature
  type definitions.
- `authorization.graphql` deleted — was a stale placeholder.

## Cleanup notes

- `notification_type` PostgreSQL enum updated from `COMMENT` to `REPLY`.
- `authorization.graphql` deleted.
- `/graphql` endpoint added to `permitAll()` alongside `/graphiql`.
