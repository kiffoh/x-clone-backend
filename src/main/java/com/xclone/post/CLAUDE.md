# Post — Entity & Feed

Phase 8 complete + getPost refactor.

## Post entity

- `Post` JPA entity in `post/model/entity/`.
- `@ManyToOne(fetch = FetchType.LAZY)` on `author` field.
- `@ForeignKey(name = PostConstraintName.POST_AUTHOR_FK)` — named FK.
- Self-referencing `@ManyToOne(fetch = FetchType.LAZY)` on `parent` field — `parentId` stored
  as a separate `@Column` with `insertable = false, updatable = false` on the `@JoinColumn` to
  allow setting FK by UUID without loading the parent entity.
- `@ForeignKey(name = PostConstraintName.POST_PARENT_FK)` — named FK for parent.
- Self-referencing `@ManyToOne(fetch = FetchType.LAZY)` on `sharedPost` field — `sharedPostId`
  stored as a separate `@Column` with `insertable = false, updatable = false`.
- `@ForeignKey(name = PostConstraintName.SHARED_POST_FK)` — named FK for shared post.
- `PostConstraintName` — constants class; includes `AUTHOR_ID_FK`, `POST_PARENT_FK`,
  `SHARED_POST_FK`.
- `parentId` is nullable — null indicates an original post (root of a reply chain).
- `sharedPostId` is nullable — null indicates a post that is not a repost or quote.
- `toPostProfile()` projects entity to `PostProfile` record including `parentId` and
  `sharedPostId`.

## Status enum

- Lives in `common/enums/Status.java`.
- Shared across entity types where `ACTIVE` / `HIDDEN` / `DELETED` semantics are identical.

## PostProfile record

- Fields include `parentId` (nullable UUID) and `sharedPostId` (nullable UUID).

## PostRepository

- `findActivePostById` — finds a single active post by ID.
- `findActivePostsById` — finds multiple active posts by a list of IDs; used by
  `@BatchMapping` for notification post resolution.
- Feed queries (`findFirstPageOfFeed`, `findNextPageOfFeed`) filter `p.parentId is null` —
  replies excluded from feed.
- `findAllReplyCountsByParentIds` — JPQL projection with `GROUP BY`; constructs `ReplyCount`
  via `select new`.
- `findFirstPageOfReplies` / `findNextPageOfReplies` — keyset pagination for direct replies.
- `findRepost(UUID sharedPostId, UUID authorId)` — filters `messageContent IS NULL` to match
  only pure reposts.
- `findShareCounts` — JPQL projection; counts all active posts sharing a `sharedPostId`.
- `findSharedIds` — JPQL projection returning `List<UUID>` of `sharedPostId` values for a user
  scoped to a post list; used by `sharedByMe` `@BatchMapping`.
- `findSharedPosts(List<UUID> sharedPostIds)` — fetches active posts by ID list for
  `@BatchMapping` shared-post resolution.
- `findFirstPageOfQuotes` / `findNextPageOfQuotes` — keyset pagination; filters
  `messageContent IS NOT NULL` to exclude pure reposts.
- `findFirstPageOfPureReposts` / `findNextPageOfPureReposts` — keyset pagination; uses
  `join fetch p.author` (avoids `LazyInitializationException`); filters
  `messageContent IS NULL`; filters author `UserStatus.ACTIVE` on both queries; returns
  `Slice<Post>` (not `Slice<User>`).
- `findAllAncestors` — native query, `WITH RECURSIVE` CTE; walks from `postId` to root via
  `parent_id`; excludes the queried post; includes deleted ancestors; ordered `created_at ASC`.
- `findAllSiblings` — JPQL; filters `Status.ACTIVE`; returns siblings created before the
  queried post; ordered `createdAt ASC`.

## PostService

- `getPost(UUID id)` — uses `findActivePostById`; returns null for both not-found and inactive.
  Consistent with `userByHandle` — client does not distinguish between non-existent and
  inactive.
- `getActivePostsFromIds(List<UUID> postIds)` — fetches multiple active posts; returns
  `List<PostProfile>`.
- `mapIfActive(Post post)` — `public static` helper; maps to `PostProfile` if `ACTIVE`,
  otherwise null. Used by `ReplyService.getReplyThread()` for ancestor null-mapping.
- `toPostConnection()` — `public static` method shared by `PostService`, `ReplyService`, and
  `ShareService`. Eliminates duplicated connection-building logic.
- `createRepost()` — toggle pattern: checks existing via `findRepost`, reactivates if
  `DELETED`, throws `DuplicateRepostException` if `ACTIVE`, creates if not found. Uses
  `findActivePostById`. `@Transactional`.
- `createQuote()` — validates shared post exists and is active; uses `findActivePostById`
  (quoting deleted posts not allowed). `@Valid` on input. `@Transactional`. No duplicate check.
- `createReply()` — validates parent exists via `findById` (not `findActivePostById` — replying
  to deleted posts intentionally allowed for thread continuity). `@Valid` on input.
  `@Transactional`.

## PostController

- `@SchemaMapping(typeName = "Post", field = "parent")` — returns null early if `parentId` is
  null (avoids unnecessary DB call); delegates to `postService.getPost()`.
- `@BatchMapping(typeName = "Post", field = "replyCount")` — maps `ReplyCount` list to
  `Map<PostProfile, Integer>` with `getOrDefault(0)`.
- `@SchemaMapping(typeName = "Post", field = "replies")` — paginated direct replies.
- `@BatchMapping(typeName = "Post", field = "sharedPost")` — batch-resolves shared posts;
  filters to non-null `sharedPostId`; missing keys resolve to null by framework.
- `@SchemaMapping(typeName = "Post", field = "quotes")` — paginated.
- `@SchemaMapping(typeName = "Post", field = "reposts")` — delegates to
  `shareService.getRepostUsers()`.
- `@BatchMapping(typeName = "Post", field = "shareCount")` — `getOrDefault(0)`.
- `@BatchMapping(typeName = "Post", field = "sharedByMe")` — retrieves auth from
  `SecurityContextHolder`; uses `getSharedIdsInPosts` scoped to batch.

## Design decisions

- **`PostNotFoundException` not thrown by queries** — reserved for mutation contexts
  (`createReply`, `createQuote`) where caller needs to act on the distinction.
- **Consolidating active-post helpers into `PostService`:** `ReplyService.getParent()` and
  `ReplyService.getActivePost()` were functionally identical to `PostService.getPost()` and
  `PostService.mapIfActive()` — duplicated logic consolidated.
- **Service method name hides implementation detail:** `createRepost` is the public name even
  though it sometimes reactivates a deleted repost.
- **`deletePost` returns `PostProfile`** — needed by `PostController` to determine post type
  and locate the original post for notification cleanup.
- **`PostType` is a private enum in `PostController`** — only used by `deletePost` to
  discern reply/quote/repost for notification cleanup; not a first-class domain concept.
- **Null guard on `getOriginalPost`** — defence-in-depth; unreachable in normal flow because
  post-deletion cascades notifications first, but guards against race conditions.

## PostFixtures

- `createReplyWithContent(String, User, UUID)` — post with parent ID.
- `createQuote(UUID, User, String)` — post with shared post ID and message content.
- `createRepost(UUID, User)` — post with shared post ID, no message content.
- `magpieRhyme` — static list of strings for test post content.

## Testing — getPost tests

- Refactored to deserialise full `PostProfile` and assert `createdAt` / `updatedAt` are
  `OffsetDateTime`.
- `getPost_invalidId_returnsNull` — nonexistent UUID returns null.
- `getPost_postDeleted_returnsNull` — soft-deleted returns null.
