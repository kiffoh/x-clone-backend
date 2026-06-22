# Follow — Vertical Slice

Phase 7 complete. Follow/unfollow with paginated followers/following lists.

## Follow entity

- `Follow` JPA entity in `follow/model/entity/`.
- `@ManyToOne(fetch = FetchType.LAZY)` on both `follower` and `following` fields.
- Unique constraint on `(follower_id, following_id)` via
  `@UniqueConstraint(name = FollowConstraintName.FOLLOW_EXISTS, ...)`.
- `@Check(name = FollowConstraintName.SELF_FOLLOW, constraints = "follower_id != following_id")`.
- **No soft delete** — follow rows are hard deleted on unfollow.
- `createdAt` only — no `updatedAt` needed.

## FollowConstraintName

- Lowercase constants to match PostgreSQL's reported constraint names.
- Uses `{domain}_constraint_{name}` convention (see `jpa-entity-conventions` skill).

## Custom exceptions

- `DuplicateFollowException`, `SelfFollowException`, `AccountNotActiveException`.
- Caught separately in controller for distinct codes.

## FollowRepository

- All pagination queries use named `@Query` methods — derived names replaced for readability.
- Method names describe what is returned, not which field is queried.
- Simple count methods (`countByFollower_Id`, `countByFollowing_Id`) remain derived — short and
  unambiguous.
- Keyset condition:
  `(f.createdAt < :cursorTimestamp) OR (f.createdAt = :cursorTimestamp AND f.id > :cursorId)`.
- `deleteByFollowerIdAndFollowingId(UUID, UUID)` — derived delete by FK values directly; no
  entity fetches; no-op when no row matches.
- `findFollowingIdsByFollowerId` — JPQL projection returning `List<UUID>` directly.
- Status filters on correct association sides: `findFirstPageOfFollowing` /
  `findNextPageOfFollowing` filter on `f.following.status`; `findFirstPageOfFollowers` /
  `findNextPageOfFollowers` filter on `f.follower.status`.

## FollowService

- `getFollowers` / `getFollowing` — paginated, return `UserConnection`.
- `followUser` — `saveAndFlush()` for immediate flush; catches
  `DataIntegrityViolationException` translated to typed exceptions; null-safety guards on
  `getRootCause()` → `getServerErrorMessage()` → `getConstraint()` chain (SpotBugs); calls
  `getActiveUserOrThrow` for both follower and following — inactive accounts cannot
  participate; `@Transactional`.
- `unfollowUser` — idempotent; calls `deleteByFollowerIdAndFollowingId` with UUIDs; calls
  `getUserOrThrow` (no status check) — operation must succeed regardless of target's status.
- `getFollowingIds` — returns `List<UUID>` wrapping `findFollowingIdsByFollowerId`.
- `getFollowingIdsInUsers` — targeted batch query; returns `Set<UUID>` scoped to a given user
  list; JPQL projection to avoid lazy association traversal; no `@Transactional` needed.
- `getUserOrThrow` — private helper; no status check.
- `getActiveUserOrThrow` — private helper; delegates to `getUserOrThrow` then enforces
  `UserStatus.ACTIVE`; used by `followUser` only.

## FollowController

- `@MutationMapping` for `followUser` and `unfollowUser`.
- `followUser` catches `AccountNotActiveException` as payload error; triggers
  `upsertNotification(FOLLOW)` with `postId = null`.
- `unfollowUser` does **not** catch `AccountNotActiveException` — status irrelevant to unfollow;
  triggers `deleteNotificationActorAndCleanupNotification(FOLLOW)` with `postId = null`.
- Malformed UUID handled as protocol error via `BindException` in `GraphQlExceptionHandler`.

## GraphQlErrorMapper additions

- `fromDuplicateFollow`, `fromSelfFollow`, `fromAccountNotActive`.

## Schema

- `followers(first: Int = 10, after: String)` and `following(first: Int = 10, after: String)`.

## Testing — complete

**FollowService unit tests:** complete.

**FollowController slice tests (`@GraphQlTest`):** `@GraphQlTest` loads all `@Controller`
beans — `UserControllerTest` must declare `@MockitoBean`s for every dependency of both
`UserController` and `FollowController`.

**FollowIT integration tests:**

`followUserTests`: `validInput_returnsUserProfile`, `existingFollow_returnsFieldError`,
`followSelf_returnsFieldError`, `invalidUserId_returnsFieldError`,
`followDeletedUser_returnsFieldError`. Invalid UUID test removed — `{@link
GlobalGraphQlExceptionHandlerTest#handlesBindException()}` documents the omission.

`unfollowUserTests`: `validInput_returnsUserProfile`,
`idempotentUnfollow_returnsUserProfile`, `invalidUserId_returnsFieldError`,
`unfollowDeletedUser_successfulDeletionOfFollow`. Invalid UUID test removed.
