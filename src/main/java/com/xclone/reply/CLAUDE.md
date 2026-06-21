# Reply — Replies & Reply Thread View

Phases 9–11 complete. Replies are posts with a non-null `parentId` — no separate
`ReplyRepository`.

## ReplyService

- Lives in `reply/service/ReplyService.java`.
- Uses `PostRepository` — all reply queries live in `PostRepository`.
- `getReplyCounts(List<UUID> postIds)` — delegates to `findAllReplyCountsByParentIds`.
- `getReplies(UUID postId, Integer first, String after)` — paginated direct replies; delegates
  to `PostService.toPostConnection()`.
- `getReplyThread(PostProfile post)` — fetches ancestors and siblings; maps ancestors through
  `PostService.mapIfActive()` (deleted → null, active → `PostProfile`); maps siblings directly
  via `Post::toPostProfile` (query already guarantees active status).

## ReplyCount record

- Lives in `reply/dto/ReplyCount.java`.
- Overloaded constructor to handle JPA `COUNT` projections returning `Long` — converts to
  `Integer` via `intValue()`.

## ReplyThread response type

- `ReplyThread` record: `ancestors: [Post]!`, `focusedPost: Post!`, `siblings: [Post!]!`.
- **Ancestors** have nullable elements — deleted ancestors render as "This post is unavailable"
  placeholders to preserve chain continuity.
- **Siblings** use non-nullable elements (`[Post!]!`) — deleted siblings filtered at query level;
  removing from a flat list doesn't break continuity.
- **`focusedPost`** is non-nullable — if a `ReplyThread` is returned at all, the focused post
  exists; entire `ReplyThread` is null when post not found.
- `focusedPost` is its own field — the viewed post is visually distinct from both ancestors and
  siblings.
- Chosen over `[[Post!]!]` (nested arrays) — named fields are self-documenting.
- Chosen over `PostConnection` — no pagination needed for ancestor/sibling lists.

## ReplyController

- `@QueryMapping` for `getReplyThread(postId: ID!): ReplyThread`.
- Returns `null` if the post is not found.
- Returns `ReplyThread(List.of(), List.of(), post)` for root posts (no parent).
- Delegates to `replyService.getReplyThread()` for posts with a parent.

## CreateReplyInput record

- Lives in `reply/dto/request/CreateReplyInput.java`.
- `@NotNull UUID parentId`, `@NotBlank` + `@Size(max = MAX_MESSAGE_CONTENT_SIZE)` on
  `messageContent`.
- Validation annotations duplicated from `CreatePostInput` — accepted decision; constants
  centralised in `ValidationConstants`; inputs may diverge independently.

## ReplyController.createReply()

- `@MutationMapping` for `createReply`.
- Catches `ConstraintViolationException` → 400, `PostNotFoundException` → 404.
- Returns `PostResponse("200", true, reply, null)`.
- Triggers `upsertNotification(REPLY)` with `parentPost.id()` as `postId`.

## Schema

- `comments` renamed to `replies`; `commentCount` renamed to `replyCount`.
- `parent: Post` — nullable, documented as null for root posts.
- `replyCount: Int!` — direct reply count.
- `replies(first: Int = 10, after: String): PostConnection!`.
- `createReply(input: CreateReplyInput!): PostResponse!`.
- `getReplyThread(postId: ID!): ReplyThread`.

## Design decisions

- **No `conversationId` / `replyThreadId` column** — `parentId` alone is sufficient for all
  current features. Can be added later if concrete need arises.
- **Quotes start their own conversation** — a quote is a new post that references via
  `sharedPostId`; does not share a thread with the original.
- **Recursive CTE over in-memory filtering** — ancestor chain scales with reply depth
  (bounded), not thread popularity (unbounded).
- **Server as source of truth for post relationships** — `getReplyThread` takes only `postId`;
  server derives `parentId` internally.
- **Siblings show only older posts** — `findAllSiblings` filters
  `createdAt < :postCreatedAt`; newer siblings accessed via `replies` field.
- **Ancestor null-mapping in Java, not SQL** — CTE returns all ancestors; `mapIfActive()` maps
  deleted to null; keeps CTE simple.
- **Sibling status filtering in SQL, not Java** — unlike ancestors, null placeholders
  unnecessary for a flat list.
- **`parent` uses `@SchemaMapping` (not `@BatchMapping`)** — replies filtered from feed via
  `parentId is null`; only resolves on single-post views; no N+1 risk.
- **Early null return in `parent` resolver** — checks `post.parentId() == null` before DB call.
- **Replying to soft-deleted posts is allowed** — `createReply` uses `findById` (not
  `findActivePostById`); deleted posts remain as placeholders in ancestor chain; thread
  continuity preserved.
- **`replyThreadId` field removed** from `Post` entity and schema.

## Testing — complete

**PostIT — replyTests:**

`parentTests`: `validReply_returnsParentForSingularReply`,
`postHasNoParent_returnsNull`, `parentIsDeleted_returnsNull`.

`replyCountTests`: `fetchingIndividualReply_noReplies` (count 0),
`fetchingIndividualReply_hasReplies` (count 2),
`fetchingIndividualReply_hasRepliesFromDeletedReplies` (count decreases),
`fetchingFeed_eachPostHasReplyCount` (batch mapping).

`repliesTests`: `noReplies_returnsEmptyPostConnection`,
`hasReplies_NoCursor_returnsPostConnection` (sorted `createdAt desc`),
`hasReplies_WithValidCursor_returnsPostConnection`,
`hasDeletedReplies_returnsPostConnection`.

**ReplyIT — getReplyThreadTests:**

- `getAllAncestorsAndSiblings_LastSibling` — 3 ancestors, 1 older sibling.
- `getAllAncestorsAndSiblings_FirstSibling` — 3 ancestors, 0 siblings.
- `getAllAncestorsAndSiblings_NoSiblings` — 2 ancestors, 0 siblings.
- `getAllAncestorsAndSiblings_DeletedSibling` — excluded from results.
- `getAllAncestorsAndSiblings_DeletedAncestor` — null placeholder in list.
- `getReplyChain_OriginalPost` — root post: empty ancestors/siblings.
- `getReplyChain_PostIdDoesNotExist` — null `ReplyThread`.

Setup: reply chain seeded in `@BeforeEach` with 6 posts across 3 users. `@AfterEach` uses
`deletePostsInDescendingOrder`. Reply chain setup moved into `getReplyThreadTests` nested
class — `createReplyTests` only needs a single parent post.

**ReplyIT — createReplyTests:**

- `validInput_returnsPostResponse`, `invalidInput_postIdDoesNotExist_returnsPostNotFound`,
  `invalidInput_messageTooLong_returnsConstraintViolation` (smoke test),
  `invalidInput_inputMissing_returnsConstraintViolation`.
- Delete reply test omission documented with `{@link PostIT.deletePostTests}`.
