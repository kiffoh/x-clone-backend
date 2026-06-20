# FK-Aware Test Cleanup

## deletePostsInDescendingOrder

- Extracted from `PostIT` inline method to `PostHelpers` as a shared `public static` helper;
  reused across `PostIT`, `ReplyIT`, and `ShareIT`.
- Iterates posts in **reverse index order** to respect parent FK constraint — loop condition
  `i >= 0` to include index 0.
- `postRepository.deleteAll()` is risky with FK constraints — JPA doesn't guarantee deletion
  order, so a parent may be deleted before its child causing a constraint violation.

## Nested class cleanup

- Nested `@BeforeEach` that needs a clean slate must explicitly delete outer `@BeforeEach`
  posts before seeding its own — the outer `@BeforeEach` inserts posts the nested class's
  `posts` field doesn't track.

## setPostStatusDeleted

- `PostHelpers.setPostStatusDeleted(Post, PostRepository)` — sets status to `DELETED` and
  flushes; replaces inline three-line pattern across tests.

## Test data helpers

- `PostHelpers.createPostContents(int)` — extracted from `PostIT` nested class method to
  shared static helper.
- `PostHelpers.seedQuotes(UUID, List<User>, List<String>, PostRepository)` — creates and saves
  quote entities.
- `PostHelpers.seedReposts(UUID, List<User>, PostRepository)` — creates and saves repost
  entities.
- `PostHelpers.seedRepost(UUID sharedPostId, UUID authorId, PostRepository)` — single repost.

## Posts field initialisation

- `posts` field in IT classes initialised to `List.of()` — prevents NPE in `cleanupDBs()`
  when `deletePostsInDescendingOrder` runs before any posts are seeded.

## Explicit ArrayList construction

- `seedReposts` returns an `ArrayList`, but callers should not rely on this. Tests that combine
  results from multiple `seedReposts` calls create a new `ArrayList<>()` explicitly rather than
  mutating the returned list. Prevents silent breakage if `seedReposts` is refactored to return
  an unmodifiable list.

## Impossible DB states

- Deleted user with active reposts is an impossible DB state — real account deletion
  soft-deletes all user's posts. Tests that set up this state are removed rather than
  maintained.
