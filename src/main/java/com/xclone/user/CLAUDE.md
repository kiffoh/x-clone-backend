# User — GraphQL Vertical Slice

Phases 4–6 complete. First GraphQL feature using vertical slice approach.
GraphiQL enabled at `/graphiql`.

## Queries

- `me: User!` — returns authenticated user's profile via `@AuthenticationPrincipal`.
- `userByHandle(handle: String!): User` — public profile lookup with `@ValidHandle` on
  service parameter; only returns `ACTIVE` users.
- `userById(id: ID!): User` — lookup by UUID, Spring coerces the string argument to UUID
  automatically; only returns `ACTIVE` users.
- `searchUsers(query: String!, first: Int, after: String): UserConnection!` — handle search
  using keyset pagination against `createdAt` and `id`; `Slice`-based to avoid count queries.
- `suggestedUsers(first: Int, after: String): UserConnection!` — returns users excluding the
  current user and all users they follow; `Slice`-based keyset pagination.

## Mutations

- `updateMyProfile` — updates profile fields; catches `DuplicateHandleException` and
  `ConstraintViolationException` as payload errors via `GraphQlErrorMapper`.
- `deleteMyAccount` — sets user status to `DELETED`; returns
  `DeleteResponse("200", true, null)`.

## Design decisions

- **Entity never returned directly** — mapped to `UserProfile` record via `toUserProfile()` on
  the entity.
- **`UserProfile` is a Java record** (immutable, no boilerplate) — the public-facing DTO.
- **`Instant` → `OffsetDateTime`** conversion via `atOffset(ZoneOffset.UTC)` in
  `toUserProfile()` for GraphQL serialisation.
- **`@Validated` on `UserService`** enables Bean Validation on method parameters.
- **`@ValidHandle` AOP proxy** not active in unit tests — only runs with Spring context.
- **`message` field removed from `UserResponse`** — redundant when errors array carries detail.
- **`code` field uses HTTP-style numeric strings** ("200", "409").
- **`UserResponse.errors` uses a single `FieldError` type** with nullable field.
- **`deleteMyAccount` returns `DeleteResponse`** not `UserResponse` — semantically cleaner, can
  evolve independently.
- **`updateProfile` loads entity fresh inside `@Transactional`** — dirty checking handles the
  write, no explicit `save()` needed.

## Pagination refactor

`searchUsers` and `suggestedUsers` refactored from `Page` to `Slice`. `totalCount` removed from
connection types — semantically misaligned with cursor pagination; follower/following counts
exposed as dedicated fields instead.

## Connection types

- `UserConnection`, `UserEdge`, `PageInfo` implemented as Java records in
  `user/dto/connection/`.
- `PageInfo` moved to `common/connection/` — shared across all connection types.
- Connection building logic lives in `toUserConnection()` — package-private static method in
  `UserService`.
- Relay cursor pagination: base64-encoded `timestamp_id` compound string.

## Schema documentation

- All types, fields, inputs, arguments, and scalars documented with `"""` descriptions.
- `DateTime` scalar documented with ISO-8601 format.
- `UpdateUserInput` at-least-one-field constraint documented at type level.
- `UserResponse` description: "Result of a mutating operation on a user."

## Controller mappings hosted here

- `@SchemaMapping` for `followers` and `following` on `User` type — follow data hangs off the
  `User` type but is resolved by `FollowService`; lives in `UserController` per controller
  mapping convention.
- `@BatchMapping` for `isFollowing` — uses `getFollowingIdsInUsers` scoped to the batch;
  returns `Map<UserProfile, Boolean>`.

## Testing — complete

**UserService unit tests:** `@ExtendWith(MockitoExtension.class)` + Mockito. Three cases for
`getUsersByHandle`: multiple users, single user, empty list.

**UserController slice tests (`@GraphQlTest`):** `@MockitoBean` for `UserService`.
`@Import(GraphQlConfig.class)` for DateTime scalar. `@WithMockCustomUser` for auth. All
mutation tests written and passing.

**UserIT integration tests:**
- Single authenticated user in `@BeforeEach`; extra users created inline.
- Cleanup called at start of `@BeforeEach`.
- `authenticatedTester()` helper builds tester with Bearer token.
- DB verification added to `deleteMyAccount` test — response alone doesn't confirm
  `UserStatus.DELETED`.
- `DeleteResponse` used as entity type for `deleteMyAccount` test.
- `schemaMappingTests` — circular follow data seeded via `FollowHelpers.seedFollowers()`;
  `isFollowing` tested on `followers` list only (following list always `true` by definition).
