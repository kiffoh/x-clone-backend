# Exception Handling — REST + GraphQL

Two completely independent execution paths: `GlobalExceptionHandler` (REST) and
`GraphQlExceptionHandler` (GraphQL). See the `graphql-conventions` skill for the full error
model rationale.

## GraphQlExceptionHandler

| Exception | Error type | Notes |
|---|---|---|
| `ConstraintViolationException` | `ValidationError` | Field from `getPropertyPath()` last segment; extensions map via `HashMap` (null values possible) |
| `BindException` | `BAD_REQUEST` | Malformed argument coercion (e.g. string → UUID); field/value from `ex.getFieldError()`; FQN `org.springframework.validation.FieldError` to avoid collision with project's `FieldError` |
| `InvalidCursorException` | `BAD_REQUEST` | Application-authored message surfaced; `WARN` without stack trace |
| `NotPostAuthorException` | `FORBIDDEN` | `WARN` without stack trace; safe here because mutations catch explicitly |
| Catch-all | `DataFetchingException` | Returns `"An unexpected error occurred"` — **never** `ex.getMessage()`; logs class + message + full stack trace |

- Two catch-all handlers are correct — REST and GraphQL are independent paths.
- Log pattern: `log.error("Unexpected error: {} - {}", ex.getClass().getSimpleName(), ex.getMessage(), ex)` — SLF4J appends stack trace when `ex` is the last argument; no `{}` needed for throwable.

## GlobalExceptionHandler

- Catch-all returns `HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()` — never
  `ex.getMessage()`.
- REST version logs request path via `request.getDescription(false)`.
- `BadCredentialsException` and `UsernameNotFoundException` merged into one 401 to prevent
  username enumeration.

## GraphQlErrorMapper

Shared mapping logic to avoid duplicating error mapping across controllers:

- `fromDuplicateHandle` — field `"handle"`.
- `fromConstraintViolations` — from `ConstraintViolationException`.
- `fromDuplicateFollow`, `fromSelfFollow`, `fromAccountNotActive`.
- `fromPostNotFound(String field, PostNotFoundException ex)` — parameterised field name.
- `fromDuplicateRepost(DuplicateRepostException ex)` — field `"sharedPostId"`, message
  `"Repost already exists"`; logs `DEBUG` with stack trace.
- `fromNotificationNotFound(NotificationNotFoundException ex)` — field `"notificationId"`.
- `fromNotNotificationRecipient(String field, NotNotificationRecipientException ex)` — takes
  field parameter; called with `"userId"`.

## Custom exceptions (`exception/custom/`)

- `AccountNotActiveException`, `DuplicateFollowException`, `DuplicateHandleException`,
  `DuplicateRepostException`, `InvalidCursorException`, `InvalidRefreshTokenException`,
  `NotNotificationRecipientException`, `NotPostAuthorException`,
  `NotificationNotFoundException`, `PostNotFoundException`, `SelfFollowException`.

## Error DTOs (`exception/dto/`)

- `ErrorResponse` — REST error shape.
- `FieldError` — with nullable field.
- `ValidationErrorResponse` — REST validation error shape.

## Testing — complete

**`GraphQlErrorMapper` unit tests:**
- `fromDuplicateHandle` — asserts size, field name, message.
- `fromConstraintViolations` — uses
  `Validation.buildDefaultValidatorFactory().getValidator()` to validate an
  `UpdateUserInput` with an invalid handle.

**`GraphQlExceptionHandler` unit tests:**
- No Spring context needed — instantiated directly.
- `handlesBindException` is `public` — required so `{@link}` references from feature ITs
  resolve at compile time; intent documented with a comment.

**`GlobalExceptionHandler` unit tests:**
- Same direct-instantiation pattern.
