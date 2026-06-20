# X-Clone Backend — Project Status

## In Progress

### Notification Slice 2 — Triggers (Phase 16)

Wiring notification creation and removal into existing action / un-action services
(`createLike`/`deleteLike`, `createRepost`/un-repost, `createQuote`, `createReply`,
`followUser`/`unfollowUser`). The shared lookup layer and the removal (cleanup) path are
complete; the creation / grouping path is implemented and awaiting tests, and the FOLLOW
removal path has an open actor-location question.

**Done:**
- Removal / cleanup path — `deleteNotificationActorAndCleanupNotification` plus shared
  `findNotification` / `findNotificationWithoutPostId` lookups and `countByNotificationId` /
  `deleteByActorUserIdAndNotificationId` actor-repo methods.
- Creation / grouping path — append-actor-vs-new-row wired into `createLike`, `createReply`,
  `createRepost`, `createQuote`, `followUser`, using the shared lookups.

**Open:**
- Confirm the FOLLOW time-window duration and where it lives (constant vs config);
  discrete-per-follow remains an acceptable first-pass fallback (the schema supports layering
  the window in later without migration — purely a change to the "find existing notification"
  lookup).
- Resolve the FOLLOW removal actor-location wrinkle — post-based notifications are uniquely
  keyed by `(recipient, type, post)`, so the unliked / un-reposted actor is guaranteed to sit
  on the located notification. FOLLOW is time-windowed with no post, so
  `findNotificationWithoutPostId` returns the *most recent* follow notification, which may
  *not* hold the unfollowing user's actor row if they followed in an earlier window — that
  user's actor row then leaks and the older notification never reaches zero from their
  unfollow. Decide how the unfollow path locates the notification that actually contains this
  actor (e.g. find the FOLLOW notification for the recipient whose actor set includes this
  user, rather than the most recent).
- Tests for the trigger paths — service-method level: one representative post-based type
  (type is an opaque filter, so the rest share the path) plus the FOLLOW branch, plus the
  type-independent edges (missing notification returns quietly, removing a non-last actor
  leaves the notification, removing the last actor deletes it). Call-site level: verify each
  un/action passes the correct `(type, postId)` — that is where copy-paste / wiring mistakes
  live, not inside the method.
- Concurrency hardening (deferred) — the count-then-delete cleanup leaves a zero-actor
  orphan under concurrent un-actions (READ COMMITTED: neither transaction sees the other's
  uncommitted actor delete). Optionally fold the emptiness test into the delete
  (`DELETE … WHERE NOT EXISTS (SELECT 1 FROM notification_actors …)`) or take a row lock.

---

## Upcoming Slices

### Notification Slice 3 — Mentions
- `post_mentions` table — own entity and repository
- `@handle` parsing — extract mentions from `messageContent`
- Trigger `MENTION` notification — independent of Slice 2

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
  logger or delete
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
