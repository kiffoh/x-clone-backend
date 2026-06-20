# Share — Reposts & Quotes

Phases 12–14 complete. "Share" is the umbrella term covering pure reposts + quotes combined.

## Terminology

- `shareCount` — combined count of pure reposts and quotes.
- `sharedByMe` — true if authenticated user has done either a pure repost or a quote.
- `reposts` — pure reposts only; returns `UserConnection` (no message content to display).
- `quotes` — quotes only; returns `PostConnection` (quotes are posts with content).
- Package renamed from `repost/` to `share/`.

## Repost (Phase 12)

### createRepost mutation

- `ShareController` — `@MutationMapping`; catches `DuplicateRepostException` → 400,
  `PostNotFoundException` → 404.
- Single `ID!` argument `sharedPostId` — no input wrapper (single field doesn't justify one).
- Returns `PostResponse("200", true, repost, null)`.

### PostService.createRepost()

- **Toggle pattern:** checks existing via `findRepost`, reactivates if `DELETED`, throws
  `DuplicateRepostException` if `ACTIVE`, creates if not found.
- Uses `findActivePostById` — reposting deleted posts not allowed (unlike `createReply`).
- `@LastModifiedDate` on `updatedAt` ensures reactivated reposts get fresh timestamp.
- `@Transactional`.

### Partial unique index

- `CREATE UNIQUE INDEX one_repost_per_user ON posts(shared_post_id, author_id) WHERE message_content IS NULL AND status = 'ACTIVE'`.
- Enforces one active repost per user per post at DB level.
- Only applies to pure reposts (`messageContent IS NULL`) — quotes not constrained.
- Only applies to active reposts — deleted reposts don't block reactivation.
- JPA annotations cannot express partial unique indexes — managed via `schema.sql`.
- `spring.jpa.defer-datasource-initialization: true` in `application-dev.yml`.

### DuplicateRepostException

- Custom `RuntimeException` for duplicate active reposts.

## Quote (Phase 13)

### CreateQuoteInput record

- Lives in `share/dto/request/CreateQuoteInput.java`.
- `@NotNull UUID sharedPostId`, `@NotBlank` + `@Size(max = MAX_MESSAGE_CONTENT_SIZE)` on
  `messageContent`.

### PostService.createQuote()

- Uses `findActivePostById` — quoting deleted posts not allowed. Unlike `createReply`, a quote
  is standalone; embedded preview of deleted post has no value.
- No duplicate check — users can quote the same post multiple times.
- `@Valid` on input. `@Transactional`.

### ShareController.createQuote()

- `@MutationMapping`; catches `ConstraintViolationException` → 400,
  `PostNotFoundException` → 404.

## Share Schema Fields (Phase 14)

### ShareService

- Lives in `share/service/ShareService.java`.
- `getRepostUsers(UUID sharedPostId, Integer first, String after)` — paginated pure reposts;
  builds `UserConnection` from `Slice<Post>` using custom `toUserConnection` that maps
  `Post::author` → `UserProfile` while encoding cursors from Post fields.
- `getQuotes(UUID sharedPostId, Integer first, String after)` — paginated quotes; delegates to
  `PostService.toPostConnection()`.
- `getShareCounts(List<UUID> postIds)` — delegates to `postRepository.findShareCounts()`.
- `getSharedIdsInPosts(List<UUID> postIds, UUID userId)` — delegates to
  `postRepository.findSharedIds()` and wraps in `HashSet` (a user who both reposts and quotes
  the same post would return duplicate IDs). Naming mirrors `getFollowingIdsInUsers`.
- `getSharedPosts(List<PostProfile> posts)` — batch lookup; filters null `sharedPostId`;
  returns early with empty list if no IDs to query. Empty list guard avoids passing empty list
  to JPQL `IN`.

### ShareCount record

- Lives in `share/dto/ShareCount.java`.
- Overloaded constructor for JPA `COUNT` → `Long` → `int`.

### Schema

- `sharedPostId: ID` — lets client distinguish "not a share" from "share whose original was
  deleted."
- `sharedPost: Post` — nullable; resolved via `@BatchMapping`.
- `quotes(first: Int = 10, after: String): PostConnection!`.
- `reposts(first: Int = 10, after: String): UserConnection!`.
- `shareCount: Int!` — combined; each repost/quote counted separately.
- `sharedByMe: Boolean!`.
- `createRepost(sharedPostId: ID!): PostResponse!`.
- `createQuote(input: CreateQuoteInput!): PostResponse!`.

## Design decisions

- **`sharedPostId` over `quotedPostId`** — both reposts and quotes use this field.
- **`sharedPost` over `originalPost`** — accurate at every level in quote chains.
- **`sharedPostId` exposed in schema** — client can distinguish "regular post" from "share
  whose original was deleted" (non-null `sharedPostId`, null `sharedPost`).
- **`findActivePostById` for both `createRepost` and `createQuote`** — on X, you can't repost
  or quote a deleted post.
- **Multiple quotes allowed** — no uniqueness constraint; quotes are original content.
- **`messageContent IS NOT NULL` filter on quote pagination** — excludes pure reposts.
- **`findRepost` filters on `messageContent IS NULL`** — ensures only pure reposts matched for
  toggle logic.
- **Self-reposts allowed** — mirrors X/Twitter.
- **Quotes independent of reposts** — a user can have both simultaneously.
- **Repost toggle (soft delete + reactivation)** — mirrors X/Twitter one-active-repost-per-user.
- **`reposts` returns `UserConnection`** — pure reposts have no content; "Reposts" tab shows
  users.
- **Pure repost pagination returns `Slice<Post>` not `Slice<User>`** — cursor uses Post's
  `createdAt`/`id`; author extracted in service layer.
- **`join fetch` for repost authors** — `join fetch p.author` in JPQL eagerly loads `User`;
  plain `join` only filters/orders and leaves association lazy.
- **Custom `UserConnection` building** — standard builder encodes from `UserProfile`; reposts
  encode from Post entity. Lives in `ShareService`.
- **Author status filter on both queries** — guards against returning inactive users.
- **`shareCount` includes both types** — mirrors X/Twitter combined count.
- **`sharedByMe` covers both types** — mirrors X/Twitter green icon.
- **Deleted user with active reposts is impossible** — account deletion soft-deletes all posts.

### Naming refactor

- `quotedPostId` → `sharedPostId`, `quotedPost` → `sharedPost`, `findQuotedPosts` →
  `findSharedPosts`, `getQuotedPosts` → `getSharedPosts` — throughout codebase (entity, DTO,
  mutation, service, repository, error mapper, schema, tests, Javadoc).

## Testing — complete

**ShareIT — createRepostTests:**

- `validInput_noExistingRepost_returnsPostResponse` — verifies repost created; 2 posts in DB.
- `validInput_existingDeletedRepost_returnsPostResponse` — reactivation; same ID returned.
- `invalidInput_existingActiveRepost_returnsDuplicateRepost` — 400.
- `invalidInput_postIdDoesNotExist_returnsPostNotFound` — 404.
- `invalidInput_inputMissing_returnsConstraintViolation` — null variable.

**ShareIT — createQuoteTests:**

- `validInput_noExistingQuote_returnsPostResponse` — 2 posts in DB.
- `invalidInput_postIdDoesNotExist_returnsPostNotFound` — 404.
- `invalidInput_inputMissing_returnsConstraintViolation` — null variable.
- Delete quote/repost test omission: class-level Javadoc references `PostIT.deletePostTests`.

**PostIT — shareTests:**

`quotesTests`: `noQuotes_returnsEmptyPostConnection`, `hasQuotes_NoCursor_…`,
`hasQuotes_WithValidCursor_…`, `hasDeletedQuotes_…`.

`sharedPostTests`: `validQuote_returnsSharedPost`, `sharedPostIsDeleted_returnsNull`,
`postIsNotAQuote_returnsNull`.

`reposts` nested class: `noReposts_returnsEmptyUserConnection`, `hasReposts_NoCursor_…`,
`hasReposts_WithValidCursor_…`, `hasDeletedReposts_…`.

`shareCount` nested class: `noShares` (0), `hasShares` (2R+2Q=4),
`hasDeletedShares` (1DR+1DQ→2), `fetchingFeed_eachPostHasShareCount` (list path assertion).

`sharedByMe` nested class: `noShares_false`, `isSharedByMe_true`,
`fetchingFeed_onePostSharedByMe`, `fetchingFeed_everyPostSharedByMe` (explicit
`new ArrayList<>()` for combining).

**Feed quote test:** `getFeed_quotesAppearInFeed_returnsPostConnection`.

**Obsolete test removed:** `hasSharesFromDeletedUser` — impossible DB state.
