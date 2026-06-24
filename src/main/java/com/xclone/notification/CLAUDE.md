# Notification — Read Side + Triggers

Phase 15 (Slice 1: read side) complete. Phase 16 (Slice 2: triggers) in progress — see
`STATUS.md` for open items.

## Notification entity

- `Notification` JPA entity in `notification/model/entity/`.
- `@Id @GeneratedValue(strategy = GenerationType.UUID)` — surrogate UUID PK.
- `recipientUserId` stored as `@Column` UUID — no entity reference needed in application code
  (always queried by authenticated user's ID).
- `User recipient` field added with `@ManyToOne(fetch = FetchType.LAZY)` +
  `@JoinColumn(insertable = false, updatable = false)` — present solely so Hibernate generates
  the FK constraint; Javadoc explains it is not used in application code.
- `postId` stored as `@Column` UUID — nullable (FOLLOW notifications have no post).
- `Post post` entity reference **removed** — post resolved via `@BatchMapping` in controller
  to avoid lazy loading issues and respect GraphQL's demand-driven resolution.
- `@Enumerated(EnumType.STRING)` on `NotificationType type`.
- `boolean read` — primitive (not boxed `Boolean`) since column is `NOT NULL DEFAULT FALSE`.
- `@CreatedDate` on `createdAt`, `@LastModifiedDate` on `updatedAt`.
- `toNotificationProfile()` — timestamps converted via `createdAt.atOffset(ZoneOffset.UTC)`
  (not `OffsetDateTime.from()` which throws `DateTimeException` on `Instant`).

## NotificationProfile record

- Fields: `UUID id`, `UUID postId` (nullable), `NotificationType type`, `boolean read`,
  `OffsetDateTime createdAt`, `OffsetDateTime updatedAt`.
- `postId` is UUID, not entity reference — JPA entities never appear in DTOs.

## NotificationActor entity

- `@Id @GeneratedValue(strategy = GenerationType.UUID)` — surrogate PK (DB composite PK
  `(actor_user_id, notification_id)` replaced for JPA simplicity).
- `actorUserId` as `@Column` UUID with `@ManyToOne User actor` via FK-by-UUID.
- `notificationId` as `@Column` UUID with `@ManyToOne Notification notification` via
  FK-by-UUID.
- `@CreatedDate` on `createdAt` — used for ordering actors within a notification.

## No @OneToMany collection on Notification

`Notification` does **not** hold `List<NotificationActor>` — adding an actor to a lazy
`@OneToMany` forces Hibernate to load the entire collection first. Expensive for viral posts.
With separate entities, adding is a single INSERT + UPDATE on `updatedAt`. One extra service
line to update `updatedAt`, but write efficiency preserved. Consistent with `Follow` pattern.

## NotificationConstraintName

- Uses `{domain}_constraint_{name}` convention for FK constraints.
- `ONE_LIKE_NOTIFICATION` / `ONE_REPOST_NOTIFICATION` — partial unique index names (not FK
  constraints, so they use descriptive index names rather than `{domain}_constraint_` prefix).
- Dead `POST_ID_FK` constant and `// Is this clear enough` comment removed.

## NotificationConstants

- `ACTOR_PREVIEW_LIMIT = 3` — limits actors per notification in Java.

## NotificationType enum

- Values: `LIKE`, `REPLY`, `REPOST`, `QUOTE`, `FOLLOW`, `MENTION`.
- `COMMENT` renamed to `REPLY`.

## NotificationRepository

- `findFirstPageOfNotifications` / `findNextPageOfNotifications` — ordered by **`updatedAt`
  desc, id asc**; cursor encodes `updatedAt` (not `createdAt`) because notifications with new
  activity should bubble to top.
- `findNotificationActors(List<UUID>)` — JPQL with `join fetch na.actor`; returns all actors
  globally ordered by `createdAt desc`; no `Pageable` — per-notification limiting done in Java.
- `findActorCounts(List<UUID>)` — JPQL projection with `GROUP BY`; constructs `ActorCount`.

## NotificationActorRepository

- `countByNotificationId(UUID)` — derived scalar count; returns `0` cleanly (no GROUP BY
  absence trap). `notificationId` is a direct `@Column`, so derived name matches column.
- `deleteByActorUserIdAndNotificationId(UUID, UUID)` — derived delete by FK columns; no-op
  when no match.
- `deleteAllByNotificationIdIn(List<UUID>)` — bulk delete for post-deletion cascade; the `In`
  keyword is required for `List` parameters in Spring Data derived queries.

## ActorCount record

- Overloaded constructor: JPA `COUNT` → `Long` → `int` via `intValue()`.

## NotificationService

- `getNotifications(UUID userId, Integer first, String after)` — paginated by `updatedAt`.
- `getMostRecentNotificationActors(List<UUID> notificationIds)` — fetches all actors globally;
  groups into `Map<UUID, List<UserProfile>>` via `computeIfAbsent` + `add`; limits to
  `ACTOR_PREVIEW_LIMIT` per notification; works because global `createdAt desc` ordering
  ensures per-notification actors arrive most-recent-first.
- `getActorCounts(List<UUID>)` — one-line delegator; Javadoc intentionally omitted.
- `readNotification(UUID userId, UUID notificationId)` — `@Transactional`; validates recipient
  matches user; sets `read = true` via dirty checking; throws `NotificationNotFoundException`
  or `NotNotificationRecipientException`.
- `toNotificationConnection()` — private; handles empty edges with null cursors in `PageInfo`.

## Slice 2 — Trigger / Cleanup (Phase 16, in progress)

### Notification lookup methods

- `findDiscreteNotification(UUID recipientId, UUID actorId, UUID postId, NotificationType type)`
  — joins on `NotificationActor` because multiple notifications share the same
  `(recipient, post, type)` for discrete types (QUOTE/REPLY/MENTION); the actor disambiguates.
- `findAggregateNotification(UUID recipientId, UUID postId, NotificationType type)` — no actor
  join; `(recipient, post, type)` uniquely identifies the notification for aggregate types
  (LIKE/REPOST). Uniqueness enforced by partial unique indexes in `schema.sql`.
- `findLastUpdatedFollow(UUID recipientId)` — returns the most recently updated FOLLOW
  notification; used by the upsert path to check if the time-window is still open.
- `findSpecificFollowNotification(UUID recipientId, UUID actorId)` — joins on actor to locate
  the FOLLOW notification containing a specific actor; used by the deletion path where
  `findLastUpdatedFollow` would miss older notifications.
- `findAllByPostId(UUID postId)` — returns all notifications referencing a post; used by
  `deletePostNotifications` for post-deletion cascade.

### deleteNotificationActorAndCleanupNotification

- Signature: `(UUID recipientId, UUID authenticatedUserId, UUID postId, NotificationType type)`.
- `@Transactional`.
- Looks up notification: `findSpecificFollowNotification` for FOLLOW (joins on actor),
  otherwise `findDiscreteNotification`.
- Returns silently when no notification found — cleanup is a side-effect of un-action; must not
  fail or roll back the surrounding operation. Differs from `readNotification` where the
  notification *is* the resource.
- Deletes actor row via `deleteByActorUserIdAndNotificationId`.
- Counts remaining actors via `countByNotificationId` **after** the delete; deletes
  notification when count is `0`. Reads post-delete state rather than inferring from pre-delete.
- Post-delete count observes the removed row because default `AUTO` flush mode flushes the
  queued delete before the count query runs — no manual flush needed.
- Named for both operations (delete actor + conditionally delete notification).

### Creation / grouping path (implemented, tested)

- `upsertNotification(UUID recipientId, UUID authenticatedUserId, UUID postId, NotificationType type)` —
  self-notification guard (`recipientId == authenticatedUserId` → return null); centralised
  here, not duplicated in controllers.
- `UPDATABLE_NOTIFICATION_TYPES` (LIKE, REPOST, FOLLOW) — look up existing notification and
  append actor. Non-updatable types (QUOTE, REPLY, MENTION) always create a new notification.
- `ALWAYS_AGGREGATE_NOTIFICATION_TYPES` (LIKE, REPOST) — always update existing notification
  regardless of time. FOLLOW uses time-windowed aggregation (`TIME_BUCKET_SECONDS = 43200`).
- Aggregate types (LIKE, REPOST) use `findAggregateNotification` — no actor join; keyed by
  `(recipient, post, type)`. Discrete types use `findDiscreteNotification` — joins on actor
  because multiple notifications share `(recipient, post, type)`.
- FOLLOW uses time-windowed model (append if within `TIME_BUCKET_SECONDS`, else new row).
- Partial unique indexes enforce one aggregate notification per `(post, recipient)` per type:
  `one_like_notification_per_recipient`, `one_repost_notification_per_recipient`. Constraint
  violations from concurrent races are swallowed — the losing thread's actor is lost (see
  concurrency limitation below).

### Post-deletion cascade

- `deletePostNotifications(UUID postId)` — deletes all notifications + actors referencing
  a post. Called when a post is soft-deleted. Mirrors X's behaviour: notifications about
  deleted posts are removed.

### Concurrency limitations (accepted)

Two TOCTOU gaps under Postgres READ COMMITTED, both accepted for learning build:

1. **Upsert path:** concurrent likes/reposts both miss `findAggregateNotification`, both try
   to create a notification. One succeeds; the other hits the partial unique index and is
   swallowed. The losing thread's actor is lost because the persistence context is
   inconsistent after a flush failure. Fix: retry actor creation in a new transaction, or
   use `INSERT … ON CONFLICT`.
2. **Deletion path:** concurrent un-actions each delete their own actor, both count a
   remaining actor, neither deletes the now-empty notification. Leaves a zero-actor orphan.
   Fix: `DELETE … WHERE NOT EXISTS (SELECT 1 FROM notification_actors …)` or row lock.

## Design decisions

- **`readNotification` is a mutation** — changes server state; queries must be side-effect-free.
- **`readNotification` uses `"userId"` as field name for 403** — communicates identity mismatch;
  acknowledged inconsistency with validation errors that use input field names.
- **403 kept distinct from 404** — semantically distinct; enumeration risk low for random UUIDs.
- **`getActorCounts` Javadoc omitted** — one-line delegator; contract handled by controller's
  `getOrDefault(0)`.
- **`ACTOR_PREVIEW_LIMIT` extracted to `NotificationConstants`** — avoids magic number.
- **Notification pagination by `updatedAt`** — new activity bubbles to top; cursor instability
  accepted (changes concentrate at top; user's cursor points into stable older territory).
- **Follow notifications use time-windowed aggregation** — post-based types have a natural
  bound (the post); follows have no post, so `(recipient, type)` alone would create an
  unbounded bucket. Time window mirrors X's "Alice and 4 others followed you" grouping.
- **Follow notification lookup must filter on type** — most recent notification could be any
  type; without type filter, follow actor could be appended to unrelated notification.
- **`countByNotificationId` over `findActorCounts` for cleanup** — `findActorCounts` GROUP BY
  returns no row for zero actors; `.getFirst()` would throw `NoSuchElementException`.
- **Notification trigger testing strategy:** test what varies (post-based vs FOLLOW branch +
  type-independent edges), not each enum value. Per-type correctness verified at call sites.
- **Post deletion cascades to notifications** — when a post is soft-deleted, all notifications
  referencing that `postId` are cleaned up (notifications + actors). Mirrors X's behaviour:
  "User B liked your tweet" is meaningless once the tweet is deleted. This also makes the
  reply/quote/repost delete path safe — if the original post is already deleted, its
  notifications are already gone, so there is nothing to clean up (null guard kept as
  defence-in-depth).

## NotificationController

- `@QueryMapping getNotifications` — passes authenticated user's ID.
- `@MutationMapping readNotification` — catches `NotNotificationRecipientException` → 403
  with `"userId"` field and `NotificationNotFoundException` → 404.
- `@BatchMapping(typeName = "Notification", field = "post")` — filters null `postId`; builds
  `postIdToPostMap` via `Collectors.toMap`; framework resolves missing keys to null.
- `@BatchMapping(typeName = "Notification", field = "actors")` — `getOrDefault(…, List.of())`.
- `@BatchMapping(typeName = "Notification", field = "actorCount")` — `getOrDefault(…, 0)`.

## NotificationResponse record

- Fields: `String code`, `Boolean success`, `NotificationProfile notification` (singular),
  `List<FieldError> errors`.

## NotificationConnection / NotificationEdge

- `NotificationConnection implements Connection<NotificationEdge>`.
- `totalCount` removed from schema.

## Custom exceptions

- `NotificationNotFoundException` — not found by ID.
- `NotNotificationRecipientException` — user is not the recipient.

## Schema

- `getNotifications(first: Int = 10, after: String): NotificationConnection!`.
- `readNotification(notificationId: ID!): NotificationResponse!` — mutation.
- `NotificationResponse` field renamed from `notifications` to `notification` (singular);
  `message` field removed.
- `notification_type` PostgreSQL enum updated from `COMMENT` to `REPLY`.

## schema.sql updates

- `follows`, `post_mentions`, `notification_actors` tables use surrogate UUID PKs with
  `id UUID PRIMARY KEY DEFAULT gen_random_uuid()` instead of composite PKs.

## Testing — Slice 1 complete

**NotificationIT:**

`GetNotificationsTests`: `noCursor_returnsNotificationConnection` (REPOST first — higher
`updatedAt`), `withValidCursor_…` (cursor pagination), `noNotifications_returnsEmpty…`.

`PostMappingTests`: `validPosts_returnsNotificationConnection`,
`followNotification_postNull` (`.valueIsNull()`).

`ActorMappingTests`: `getActors_…` (2 actors per notification, `createdAt desc`),
`truncatesActorsTo3_…` (4 seeded, 3 returned, `actorCount` is 4).

`ReadNotificationTests`: `notificationNotRead_returns…` (200, read=true),
`notificationAlreadyRead_returns…` (idempotent), `invalidId_returnsNotFound` (404),
`userNotRecipient_returnsForbidden` (403 with `"userId"`).

**Test fixtures:**
- `NotificationFixtures.createNotification()` — manual `createdAt`.
- `NotificationFixtures.createNotificationActor()` — manual `createdAt`.
- `NotificationHelpers.seedNotifications()` — actors spaced `now.plusSeconds(i)` for
  deterministic ordering.
