# Keyset Query Inventory

All keyset pagination queries follow the same two-method pattern:
`findFirstPageOfX` (no cursor) and `findNextPageOfX` (with cursor condition).

## Query pairs by feature

### User — searchUsers / suggestedUsers
- `searchUsers` — handle search using keyset pagination against `createdAt` and `id`;
  `suggestedUsers` — returns users excluding current user and all followed users.
- Both use `Slice`-based keyset pagination. Refactored from `Page` to `Slice`; `totalCount`
  removed.

### Follow — followers / following
- `findFirstPageOfFollowing` / `findNextPageOfFollowing` — filter on `f.following.status`
  (status of the users being followed).
- `findFirstPageOfFollowers` / `findNextPageOfFollowers` — filter on `f.follower.status`
  (status of the users doing the following).
- Status filters on correct association sides.
- Schema default: `followers(first: Int = 10, after: String)`.

### Post — feed
- `findFirstPageOfFeed` / `findNextPageOfFeed` — filter `p.parentId is null` (replies excluded
  from feed).
- Ordered by `createdAt desc, id asc`.

### Reply — direct replies
- `findFirstPageOfReplies` / `findNextPageOfReplies` — keyset pagination for direct replies
  to a post.
- Ordered by `createdAt desc, id asc`.

### Share — quotes
- `findFirstPageOfQuotes` / `findNextPageOfQuotes` — keyset pagination for direct quotes.
- Filters on `messageContent IS NOT NULL` to exclude pure reposts.
- Ordered by `createdAt desc, id asc`.

### Share — pure reposts (UserConnection)
- `findFirstPageOfPureReposts` / `findNextPageOfPureReposts` — returns `Slice<Post>` (not
  `Slice<User>`) because cursor is based on Post's `createdAt`/`id`.
- Uses `join fetch p.author` to eagerly load author data (avoids
  `LazyInitializationException`).
- Filters on `messageContent IS NULL` to exclude quotes.
- Filters on author `UserStatus.ACTIVE` on both first and next page queries.
- Author extracted in service layer for `UserConnection` building.

### Notification — getNotifications
- `findFirstPageOfNotifications` / `findNextPageOfNotifications` — ordered by **`updatedAt`
  desc, id asc** (not `createdAt`); notifications with new activity bubble to top.
- Cursor encodes `updatedAt`, not `createdAt`.

## Keyset condition pattern

All next-page queries use:
```sql
(f.createdAt < :cursorTimestamp)
OR (f.createdAt = :cursorTimestamp AND f.id > :cursorId)
```
(Substitute `updatedAt` for notifications.)

## Repository method naming

- All pagination queries use named `@Query` methods — derived method names replaced for
  readability.
- Method names describe what is returned to the caller, not which field is queried internally.
- Simple count methods (`countByFollower_Id`, `countByFollowing_Id`) remain as derived names —
  short and unambiguous.
