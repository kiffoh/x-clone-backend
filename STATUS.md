# X-Clone Backend — Project Status

## In Progress

### Notification Slice 2 — Triggers (Phase 16)

Wiring notification creation and removal into existing action / un-action services
(`createLike`/`deleteLike`, `createRepost`/un-repost, `createQuote`, `createReply`,
`followUser`/`unfollowUser`). Creation, removal, and integration tests are complete.
Only concurrency hardening remains open.

**Done:**
- Removal / cleanup path — `deleteNotificationActorAndCleanupNotification` plus
  `findDiscreteNotification` / `findSpecificFollowNotification` lookups and
  `countByNotificationId` / `deleteByActorUserIdAndNotificationId` actor-repo methods.
- Creation / grouping path — `upsertNotification` wired into `createLike`, `createReply`,
  `createRepost`, `createQuote`, `followUser`; uses `findAggregateNotification` (LIKE/REPOST),
  `findLastUpdatedFollow` (FOLLOW), or creates new (discrete types).
- FOLLOW time-window confirmed at 12 hours — `NotificationConstants.TIME_BUCKET_SECONDS`.
- FOLLOW removal resolved — `findSpecificFollowNotification` joins on actor to locate the
  correct notification across time-window boundaries.
- Discrete-type (QUOTE/REPLY) removal resolved — `findDiscreteNotification` joins on actor
  (`na.actorUserId = :actorId`) so deletion targets the correct per-user notification.
- Aggregate vs discrete query split — `findAggregateNotification` (no actor join, keyed by
  `recipient/post/type`) for LIKE/REPOST upsert; `findDiscreteNotification` (with actor join)
  for QUOTE/REPLY/MENTION deletion.
- Partial unique indexes — `one_like_notification_per_recipient` and
  `one_repost_notification_per_recipient` enforce one aggregate notification per
  `(post, recipient)` per type; concurrent race violations swallowed with `@Slf4j` logging.
- Self-notification guard centralised in `upsertNotification` — redundant controller-level
  checks removed from `LikeController`, `ReplyController`, `ShareController`.
- `ClockConfig` bean + `@MockitoBean Clock` — deterministic time-based testing for
  time-window logic.
- Integration tests for all trigger paths — `NotificationIT.NotificationTriggers` covers
  upsert (FOLLOW inside/outside time bucket, LIKE, REPOST, QUOTE, REPLY) and deletion
  (unfollow, unlike, delete repost/quote/reply with single and multiple actors).
- Post-deletion cascade — `deletePostNotifications` removes all notifications + actors
  referencing a deleted post; null guard on `getOriginalPost` as defence-in-depth.
- `PostType` scoped as private enum in `PostController` — no cross-package dependency.

**Open:**
- Concurrency hardening (deferred) — two TOCTOU gaps under READ COMMITTED:
  1. **Upsert path:** concurrent likes/reposts both miss `findAggregateNotification`, both try
     to create → one hits the partial unique index (`one_like_notification_per_recipient` /
     `one_repost_notification_per_recipient`). Currently swallowed — the losing thread's actor
     is lost because the persistence context is inconsistent after a flush failure. Fix: retry
     actor creation in a new transaction, or use `INSERT … ON CONFLICT`.
  2. **Deletion path:** concurrent un-actions each delete their own actor, both count a
     remaining actor, neither deletes the now-empty notification. Leaves a zero-actor orphan.
     Fix: `DELETE … WHERE NOT EXISTS (SELECT 1 FROM notification_actors …)` or row lock.

---

## Upcoming Slices

### Notification Slice 3 — Mentions (Phase 17, nearly complete)

Read-side, write-side CRUD, MENTION notification triggers, and integration tests all complete.
Only FK constraints and `@handle` parsing remain.

**Done:**
- `Mention` entity (`post_mentions` table) with FK-by-UUID pattern.
- `PostMention` flat projection record for JPQL → service aggregation.
- `MentionRepository.findPostMentions` — JPQL join to `User`.
- `MentionRepository.deleteByPostIdAndMentionedUserIdIn` — batch delete.
- `MentionService.getPostMentions` — groups flat rows into `Map<UUID, List<UserProfile>>`.
- `MentionService.createMentions` — batch active-user check via `findAllActiveUsersByIdIn`;
  returns filtered list of user IDs for which mentions were created.
- `MentionService.updateMentions` — diffs mentions; reconstructs `MentionDiff` with filtered
  `added()` so callers only trigger notifications for active users.
- `MentionService.deleteMentions` — single bulk delete query.
- `MentionDiff` record — immutable diff; uses `List.of()` (not null) for unchanged state.
- `PostController` `@BatchMapping` for `mentions` field.
- Schema updated: `mentions: [User!]!` with `"""` description.
- `mentionedUserIds: [ID!]` wired on `CreatePostInput`, `UpdatePostInput`, `CreateReplyInput`,
  `CreateQuoteInput` into mention creation + MENTION notification triggers.
- MENTION notification triggers on create (post/reply/quote) and update (post).
- MENTION notification cleanup on update (removed mentions) and delete (all post types).
- `deletePostNotifications` now called unconditionally (not just for pure POST type).
- `deleteNotificationActorAndCleanupNotification` parameter order refactored to match
  `upsertNotification`: `(recipientId, authenticatedUserId, postId, type)`.
- Javadoc updated on all mutation controller methods to document notification/mention side
  effects.
- `MentionFixtures.createMention` + `PostHelpers.seedMention` + `PostHelpers.addPostWithMentions`
  — test fixtures and helpers.
- `BaseGraphQLIntegrationTest` — added `MentionRepository` autowiring and `@BeforeEach` cleanup.
- Full integration test coverage: `MentionIT` (create + update CRUD), `PostIT.mentionTests`
  (`@BatchMapping` read-side), `NotificationIT` (MENTION creation + deletion triggers),
  `ValidationIT` (`mentionedUserIds` on all input types).

**Open:**
- `MentionConstraintName` — FK constraint name constants.
- `@ManyToOne` entity references on `Mention` (User, Post) with named FK constraints.
- `@handle` parsing from `messageContent`.

### Notification Slice 4 — Subscriptions
- Real-time push via WebSocket — Spring GraphQL subscriptions
- WebSocket authentication — new infrastructure

---

## Refactors

- Hardcode field in `fromNotNotificationRecipient` — remove `field` parameter; will always be
  `"userId"`
- Add `IF NOT EXISTS` to `schema.sql` index and type creation statements — prevents errors on
  re-runs
- Remove `replyThreadId` from `Post` entity, `PostProfile`, `PostFixtures`, schema, and
  tests — clean up the removed field
- Remove `System.out.println` from `createFeed()` — left over from debugging; replace with
  logger or delete (notification `System.out.println` already removed in XC-90)
- Add `"Authentication: Required."` to remaining query endpoints — `userByHandle`, `userById`,
  `searchUsers`, and any other protected queries missing the annotation
- Fix `getNotifications` schema description — currently says "ordered by creation time" but
  notifications are ordered by `updatedAt`
- Page-size bounds validation — cross-cutting concern for all paginated endpoints;
  `Pageable.ofSize(first)` throws `IllegalArgumentException` on `first <= 0`; no upper bound
  cap; schema default prevents null but client can send 0, negative, or very large values;
  decide whether to enforce bounds centrally or per-endpoint
- Actor query performance — `findNotificationActors` loads all actors to keep
  `ACTOR_PREVIEW_LIMIT` per notification; if this becomes a hot path, consider
  `ROW_NUMBER() OVER (PARTITION BY notification_id ORDER BY created_at DESC)` window function
  in a native query filtered to rank ≤ `ACTOR_PREVIEW_LIMIT`; add a one-line comment in the
  service noting the tradeoff

---

## Investigation

- Verify `@CreatedDate` auditing behaviour in test fixtures — `NotificationFixtures` sets
  `createdAt` manually (`now.plusSeconds(i)`), but `@CreatedDate` + `AuditingEntityListener`
  may overwrite the value with the real wall-clock time on save; if so, the deterministic
  1-second spacing between actors is illusory and ordering tests depend on insertion-order
  coincidence; the `isAfterOrEqualTo` changes in `PostIT`/`ReplyIT` confirm that equal
  timestamps already occur for sequential saves; verify by saving an entity with a distinctive
  `createdAt`, reading it back, and checking whether the value survived; if auditing clobbers,
  consider removing `@CreatedDate` from test entities or using `saveAndFlush` with explicit
  `Thread.sleep` for deterministic spacing

---

## Remaining Unit/Slice Tests from Earlier Slices

- Unit tests for `PostService.createRepost` — toggle logic (create, reactivate, duplicate)
- Unit tests for `PostService.createQuote` — active post check, entity creation
- Controller slice tests for `ShareController` — `createRepost` and `createQuote` mutations
  with mocked `PostService`
- Unit tests for `ShareService` — `getQuotes`, `getSharedPosts`, `getRepostUsers`,
  `getShareCounts`, `getSharedIdsInPosts`
- Unit tests for `GraphQlErrorMapper.fromDuplicateRepost`
- Unit tests for `GraphQlErrorMapper.fromNotificationNotFound` and
  `fromNotNotificationRecipient`
- Unit tests for `NotificationService` — `getNotifications`, `readNotification`,
  `getMostRecentNotificationActors`, `getActorCounts`
- Controller slice tests for `NotificationController` — `getNotifications`,
  `readNotification`, `actors`, `actorCount`, `post` batch mappings
- Javadoc pass for share slice — `ShareController`, `ShareService`,
  `DuplicateRepostException`, `PostService.createRepost`, `PostService.createQuote`
- Javadoc pass for notification slice — `NotificationController`, `NotificationService`,
  `NotificationNotFoundException`, `NotNotificationRecipientException`, and Slice 2 trigger /
  cleanup code (`deleteNotificationActorAndCleanupNotification`, the create / grouping path,
  the new repository methods)
- Update `PostService` and `ReplyService` unit tests — reflect the refactored `getPost`,
  removal of `getParent` and `getActivePost`, and addition of `mapIfActive`
- Update `PostController` slice tests — `parent` resolver now delegates to
  `postService.getPost()` instead of `replyService.getParent()`; add `sharedPost`, `quotes`,
  `reposts`, `shareCount`, `sharedByMe` resolver mocks for `ShareService` dependency; add
  `post` batch mapping mock for `NotificationController` dependency
