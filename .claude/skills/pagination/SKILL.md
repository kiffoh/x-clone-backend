---
name: pagination
description: Cursor-based keyset pagination engine for the X-Clone backend — covers the Cursor record (common/connection/Cursor.java), Connection interface, PageInfo, UserConnection, PostConnection, NotificationConnection, Relay cursor encoding, Slice-vs-Page rationale, toPostConnection/toUserConnection builders, and keyset query patterns across all paginated endpoints.
---

# Pagination — Cursor / Keyset / Connection

## Cursor record (`common/connection/Cursor.java`)

- Encodes `Instant timestamp` + `UUID id` as a base64 string `"timestamp_id"`.
- `_` used as delimiter — safe since neither `Instant` nor `UUID` contain it.
- `timestamp` field name chosen over `createdAt` to be agnostic — some cursors encode
  `createdAt`, others encode `updatedAt` (e.g. notifications).
- `encode()` — instance method, uses `StandardCharsets.UTF_8` for consistent encoding across
  environments; Javadoc references `{@code timestamp_id}` format.
- `toCursor(String)` — static factory method. Validates `parts.length == 2` after split before
  attempting to parse — guards against `ArrayIndexOutOfBoundsException` when the decoded string
  contains no delimiter.
- All decoding failures throw `InvalidCursorException` with a fixed application-authored
  message — JDK exception messages are never surfaced.

## Connection interface (`common/connection/Connection.java`)

- Generic interface `Connection<T>`.
- Implementations: `UserConnection`, `PostConnection`, `NotificationConnection`.
- `PageInfo` lives in `common/connection/` — shared across all connection types.
- `totalCount` removed from all connection types — semantically misaligned with cursor
  pagination. Follower/following counts exposed as dedicated fields instead.

## Keyset pagination pattern

- Relay cursor pagination: base64-encoded `timestamp_id` compound string.
- `Slice`-based (not `Page`) — avoids count queries. `SliceImpl<>(list, pageable, hasNext)` is
  the concrete implementation; third argument controls whether a next page exists.
- All paginated queries come in first-page / next-page pairs:
  - First page: no cursor, ordered by `createdAt desc, id asc` (or `updatedAt desc, id asc`
    for notifications).
  - Next page: keyset condition `(f.createdAt < :cursorTimestamp) OR (f.createdAt = :cursorTimestamp AND f.id > :cursorId)`.
- Notification pagination is ordered by `updatedAt desc` — notifications with new activity
  bubble to the top. Theoretical cursor instability from mutable `updatedAt` is accepted because
  changes concentrate at the top (recent notifications) while the cursor points into older,
  more stable territory.
- Schema defaults: `first: Int = 10` on all paginated fields.

## Connection builders

- `PostService.toPostConnection()` — `public static` method shared by `PostService`,
  `ReplyService`, and `ShareService` to eliminate duplicated connection-building logic.
  Extracted at the point where duplication first appeared.
- `ShareService` has a custom `toUserConnection` for pure reposts — maps `Post::author` →
  `UserProfile` while encoding cursors from Post fields (repost pagination is ordered by
  Post's `createdAt`/`id`, not User's).
- `NotificationService.toNotificationConnection()` — private method; handles empty edge list
  with null cursors in `PageInfo`.

## Cross-cutting decisions

- **Malformed cursor is a cross-cutting concern:** tested once in
  `ValidationIT.MalformedCursorTests`, not per paginated endpoint. All endpoints go through
  `Cursor.toCursor` → `InvalidCursorException` → `GraphQlExceptionHandler`.
- **Feed excludes replies:** `findFirstPageOfFeed` / `findNextPageOfFeed` filter on
  `p.parentId is null` — only root posts appear in the feed.
- **No separate `ReplyRepository`:** replies are posts with a non-null `parentId`. All reply
  queries live in `PostRepository` to avoid an unnecessary repository layer.
- **Page-size bounds validation:** open refactor item — `Pageable.ofSize(first)` throws
  `IllegalArgumentException` on `first <= 0`; no upper bound cap yet.

See [references/keyset-queries.md](references/keyset-queries.md) for the full query inventory.
