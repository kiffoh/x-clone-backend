# @SchemaMapping vs @BatchMapping — Decision Rationale

## Rule of thumb

- **`@SchemaMapping`** when the field only resolves on single-item views (no list context, no
  N+1 risk).
- **`@BatchMapping`** when items appear in a list (feed, followers, notifications) — batching
  avoids N+1.
- **`@BatchMapping` (not JPA eager fetching)** always — GraphQL is demand-driven; eager
  fetching at JPA level forces a join on every load regardless of whether the client requested
  the field.

## Specific decisions

### `parent` on Post → `@SchemaMapping`

Replies are filtered out of the feed via `parentId is null`. The `parent` field only resolves
on single-post views (reply thread), so there is no N+1 risk in practice. Controller checks
`post.parentId() == null` before calling `postService.getPost()` — avoids a pointless
`WHERE p.id = NULL` database round trip for root posts.

### `sharedPost` on Post → `@BatchMapping`

Unlike `parent`, quotes appear in the feed alongside regular posts. A feed page with multiple
quotes would trigger N+1 queries with `@SchemaMapping`. `@BatchMapping` collects all posts in
the batch, filters to those with a non-null `sharedPostId`, and resolves them in a single query.

### `@BatchMapping` return convention

Only entries with non-null results are put in the map — missing keys resolve to `null`
automatically by the GraphQL framework. No need to explicitly put null values. Same pattern
across `sharedPost`, `isFollowing`, `post` (notification).

### `isFollowing` → `@BatchMapping` in `UserController`

Uses `getFollowingIdsInUsers` scoped to the batch; returns `Map<UserProfile, Boolean>`.

### `replyCount` → `@BatchMapping` in `PostController`

Batch-resolves reply counts; maps `ReplyCount` list to `Map<PostProfile, Integer>` with
`getOrDefault(0)` for posts with no replies.

### `shareCount` → `@BatchMapping` in `PostController`

Batch-resolves combined repost + quote counts; same `getOrDefault(0)` pattern.

### `sharedByMe` → `@BatchMapping` in `PostController`

Retrieves `CustomUserDetails` from `SecurityContextHolder`; uses `getSharedIdsInPosts` scoped
to the batch; returns `Map<PostProfile, Boolean>`.

### `actors` / `actorCount` / `post` on Notification → `@BatchMapping` in `NotificationController`

- `actors` — uses `getOrDefault(notification.id(), List.of())`.
- `actorCount` — uses `getOrDefault(notification.id(), 0)`.
- `post` — filters out null `postId` values (FOLLOW type) before JPQL `IN` clause; builds map
  via `Collectors.toMap`; framework resolves missing keys to null.

## Follow data hangs off User type

`@SchemaMapping` for `followers` and `following` on `User` type — follow data lives in
`UserController` per the controller mapping convention, resolved by `FollowService`.
