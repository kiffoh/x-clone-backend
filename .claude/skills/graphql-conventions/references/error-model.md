# Error Model — Errors-as-Data vs Protocol Errors

## Core split

- **Mutations** model operations with known failure modes — these belong in the mutation
  response payload (`XResponse.errors`).
- **Queries** return plain domain types with no errors field — validation failures and
  not-found cases bubble as protocol-level GraphQL errors.
- `GraphQlExceptionHandler` is a last resort — for unexpected exceptions on any resolver and
  for expected exceptions on query resolvers; must never leak exception messages.

## GraphQlExceptionHandler handlers

| Exception | Resolver type | Error type | Notes |
|---|---|---|---|
| `ConstraintViolationException` | Queries | `ValidationError` | Field name from `getPropertyPath()` last segment; field in extensions map via `HashMap` (not `Map.of()` — null values possible) |
| `BindException` | Queries | `BAD_REQUEST` | Malformed GraphQL argument values (e.g. string → UUID coercion failure); field name and rejected value from `ex.getFieldError()` |
| `InvalidCursorException` | Queries | `BAD_REQUEST` | Message surfaced directly (application-authored); logged `WARN` without stack trace |
| `NotPostAuthorException` | Queries only (`likes`) | `FORBIDDEN` | Logged `WARN` without stack trace; safe here because mutations catch explicitly before it reaches handler |
| Catch-all | Any | `DataFetchingException` | Returns `"An unexpected error occurred"` — never `ex.getMessage()`; logs class, message, and full stack trace |

## GlobalExceptionHandler (REST)

- Catch-all returns `HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()` — never
  `ex.getMessage()`.
- REST version logs the request path via `request.getDescription(false)` which the GraphQL
  handler cannot access.

## Mutation payload error conventions

- `code` field uses HTTP-style numeric strings ("200", "400", "403", "404", "409").
- `message` field removed from `UserResponse` — redundant when errors array carries detail.
- `errors` uses a single `FieldError` type with a nullable field.
- `GraphQlErrorMapper` centralises mapping logic to avoid duplication across controllers.

## Errors-as-data for mutation authorization failures

`readNotification` catches `NotNotificationRecipientException` as errors-as-data (403 in
payload), consistent with `updatePostContent`/`deletePost` handling
`NotPostAuthorException`. Mutations catch business exceptions explicitly; queries let them
bubble to the protocol handler.

## 403 vs 404 distinction for readNotification

The two failures are semantically distinct. Enumeration risk is low for notifications (random
UUID IDs, low-sensitivity resource). Unlike the auth layer's merged 401 (which prevents
username enumeration on a high-value target), notification existence leakage is acceptable.

`readNotification` uses `"userId"` as field name for 403 — deliberate choice communicating
identity mismatch. Acknowledged inconsistency with validation errors (which use input field
names). Decision made with awareness of the tradeoff.

## Log patterns

- `log.error("Unexpected error: {} - {}", ex.getClass().getSimpleName(), ex.getMessage(), ex)`
  — SLF4J appends full stack trace automatically when `ex` is the last argument; no `{}`
  placeholder needed for the throwable itself.
- Client errors (`InvalidCursorException`, `NotPostAuthorException`) logged at `WARN` without
  stack trace — they are not application failures.
