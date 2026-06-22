# GraphQlTester Patterns

## @GraphQlTest setup

- `@GraphQlTest` loads a lightweight GraphQL slice — service dependencies are `@MockitoBean`.
- `@GraphQlTest` loads **all** `@Controller` beans — if multiple controllers exist, each test
  class must declare `@MockitoBean`s for every dependency of every controller loaded (e.g.
  `UserControllerTest` must mock `FollowService` even though it only tests `UserController`).
- `@Import(GraphQlConfig.class)` required to register `DateTime` scalar.
- Authentication: `@WithMockCustomUser` annotation backed by
  `WithSecurityContextFactory<WithMockCustomUser>`.

## Input variables

- Input variables must always be passed as `Map`, never as typed Java objects.
- `Map.of()` does not allow null values — use `HashMap` when any input field may be null.
- GraphQL variables must be declared in the operation signature with their type before they
  can be referenced.

## Selection sets

- The selection set in the document must include **every** field being asserted — missing
  fields silently return null.

## Stub patterns

- Stubs using `when(service.method(exactArg))` are fragile in controller slice tests — prefer
  `anyString()`, `any()` matchers.

## Connection path extraction

Extracting a single entity from a `Connection` requires navigating the path to the node:

```
.path("getNotifications.edges[0].node").entity(NotificationProfile.class)
```

Calling `.path("getNotifications").entity(NodeDto.class)` decodes the connection root into the
node DTO, which **silently yields an object with all-null fields** rather than an error — the
field names don't match, so nothing binds. A null assertion failure on the first asserted field
is usually a symptom of this path issue or a missing selection-set field, not a server-side bug.

## Assertion patterns

- `matchesJson` uses **lenient** matching by default — explicitly asserting `"errors": null`
  is the stricter, preferred approach.
- `SliceImpl<>(list, pageable, hasNext)` is the concrete implementation used to construct
  `Slice` instances in tests — third argument controls whether a next page exists.
- Assert `isAfterOrEqualTo` / `isBeforeOrEqualTo` instead of strict `isAfter` / `isBefore` —
  sequential saves can produce equal timestamps; ordering still guaranteed by `id asc`
  tiebreaker in keyset pagination queries.
