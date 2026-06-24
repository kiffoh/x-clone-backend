# Mention — Post Mentions (In Progress)

Phase 17 (Notification Slice 3). Read-side `@BatchMapping`, write-side CRUD, and MENTION
notification triggers complete. FK constraints and `@handle` parsing not yet implemented.

## Domain placement

Mention has its own vertical slice (`mention/`), not inside `notification/`. The
`post_mentions` table FKs point to `posts` and `users`, not `notifications` — same pattern as
every other trigger source (like, follow, share, reply) having its own domain and calling into
`NotificationService`.

## Mention entity

- `Mention` JPA entity in `mention/model/entity/`.
- `@Id @GeneratedValue(strategy = GenerationType.UUID)` — surrogate UUID PK.
- `postId` stored as `@Column` UUID — FK-by-UUID pattern.
- `mentionedUserId` stored as `@Column` UUID — FK-by-UUID pattern.
- `@CreatedDate` on `createdAt`.
- No `@ManyToOne` entity references yet — to be added with FK constraint names when
  `MentionConstraintName` is created.

## MentionDiff record

- Lives in `mention/dto/MentionDiff.java`.
- Immutable diff between updated and current mention sets: `isChanged`, `added`, `removed`.
- `MentionDiff.of(Set<UUID> updated, Set<UUID> current)` — factory method; returns
  `List.of()` for both lists when unchanged (not null).
- `updateMentions` reconstructs the `MentionDiff` after `createMentions` filtering so that
  `added()` reflects only users for whom mention rows were actually created (active users).

## PostMention record

- Lives in `mention/dto/PostMention.java`.
- JPQL projection record: `PostMention(UUID postId, User user)` — flat, one row per mention.
- `User` entity reference (not `UserProfile`) because JPQL `new` constructs from the joined
  entity; mapping to `UserProfile` happens in the service layer.

## MentionRepository

- `findPostMentions(List<UUID> postIds)` — JPQL with `join User u on m.mentionedUserId = u.id`;
  filters `u.status = UserStatus.ACTIVE` (deleted/suspended users excluded from mention
  results — mirrors X behaviour where deactivated profiles become unlinkable);
  returns `List<PostMention>` (flat projection, one row per mention).
- `findAllByPostId(UUID postId)` — derived query; returns all mentions for a post; used by
  `updateMentions` to compute the diff against current state.
- `deleteByPostIdAndMentionedUserIdIn(UUID postId, Collection<UUID> mentionedUserIds)` —
  derived bulk delete; single query for all removed mentions.

## MentionService

- `getPostMentions(List<UUID> postIds)` — groups flat `PostMention` rows into
  `Map<UUID, List<UserProfile>>` via `Collectors.groupingBy` + `Collectors.mapping`.
  Returns `Map<UUID, List<UserProfile>>` — same shape as
  `NotificationService.getMostRecentNotificationActors`.
- `createMentions(UUID postId, List<UUID> mentionedUserIds)` — batch-fetches active users via
  `findAllActiveUsersByIdIn`, creates mention rows only for active users, returns the filtered
  list of user IDs. `@Transactional`.
- `updateMentions(UUID postId, List<UUID> updatedMentionedUserIds)` — diffs updated vs current
  mentions via `MentionDiff.of`; delegates to `createMentions` / `deleteMentions`; reconstructs
  `MentionDiff` with filtered `added()` list so callers only trigger notifications for users
  whose mention rows were actually created. `@Transactional`.
- `deleteMentions(UUID postId, List<UUID> mentionedUserIds)` — single bulk delete via
  `deleteByPostIdAndMentionedUserIdIn`. `@Transactional`.

## Controller integration

### Read-side
- `PostController` `@BatchMapping(typeName = "Post", field = "mentions")` — batch-resolves
  mentions for all posts; `getOrDefault(post.id(), List.of())` for posts with no mentions.

### Write-side — mention creation + MENTION notification trigger
- `PostController.createPost` — if `mentionedUserIds` provided, creates mentions and triggers
  `MENTION` notification for each active mentioned user.
- `PostController.updatePostContent` — if `mentionedUserIds` provided (including empty list),
  diffs mentions; triggers `MENTION` notification for added users, deletes notifications for
  removed users. `null` list skips mention processing entirely.
- `PostController.deletePost` — `deletePostNotifications` called unconditionally for all post
  types, cleaning up mention notifications alongside other post-related notifications.
- `ReplyController.createReply` — same mention creation + MENTION notification pattern as
  `createPost`, plus the existing REPLY notification trigger.
- `ShareController.createQuote` — same mention creation + MENTION notification pattern as
  `createPost`, plus the existing QUOTE notification trigger.

## Schema

- `mentions: [User!]!` on `Post` type — `"""` triple-quote description.
- `mentionedUserIds: [ID!]` on `CreatePostInput`, `UpdatePostInput`, `CreateReplyInput`,
  `CreateQuoteInput` — optional; wired into mention creation.

## Design decisions

- **Filtered `MentionDiff` on update** — `updateMentions` reconstructs `added()` with only the
  IDs returned by `createMentions` (active users). Without this, the controller would trigger
  notifications for inactive/non-existent users whose mention rows were skipped. The create
  path already had this right (iterates `createdMentionUserIds`); the update path needed the
  same filtering.
- **`MentionDiff` uses `List.of()` not `null` for unchanged state** — callers don't need to
  null-check before iterating; fail-safe API at zero cost.
- **Batch delete** — `deleteByPostIdAndMentionedUserIdIn` issues a single DELETE query instead
  of N individual deletes in a loop.
- **Batch active-user check** — `createMentions` uses `findAllActiveUsersByIdIn` (single query)
  instead of per-user `existsByIdAndUserStatusActive` calls.
- **`deletePostNotifications` called for all post types** — previously only called for pure
  POST type; now unconditional so mention notifications are cleaned up when any post type
  (reply, quote, repost) is deleted. No double-delete risk because other notification types
  (QUOTE/REPLY/REPOST) reference the original post's ID, not the deleted post's ID.

## Not yet implemented

- `MentionConstraintName` — FK constraint name constants.
- `@ManyToOne` entity references on `Mention` (User, Post) with named FK constraints.
- `@handle` parsing from `messageContent`.

## Testing — complete

**MentionIT — CreateMention:**
- `createMention_createPost_success`, `createMention_createReply_success`,
  `createMention_createQuote_success` — mention row created with correct post/user IDs.
- `createMention_noMentionedUsers` — empty list, no mention rows.
- `createMention_inactiveUser_skippedSilently` — non-existent user filtered; only active user
  gets mention row.
- `createMention_multipleMentionedUsers` — two mentions created, correct IDs.

**MentionIT — UpdateMention:**
- `updateMention_nullMentionedUserIds_skipsUpdate` — null list preserves existing mentions.
- `updateMention_sameMentionedUsers` — no-op when sets match.
- `updateMention_addsMentionedUsers` — new mention added alongside existing.
- `updateMention_deletesMentionedUsers_lessMentionedUserIdsSent` — mention removed.
- `updateMention_deletesMentionedUsers_emptyMentionedUserIdsSent` — all mentions removed.
- `updateMention_addsAndDeletesMentionedUsers` — simultaneous add and remove.
- `updateMention_addsInactiveUser_skippedSilently_noNotificationCreated` — inactive user
  filtered from both mention creation and notification triggering.

**PostIT — mentionTests:**
- `getPostWithNoMentions_returnsPostProfile`, `getPostWithOneMention_returnsPostProfile`,
  `getPostWithMultipleMentions_returnsPostProfile`,
  `getPostWithDeletedMentionedUser_excludesDeletedUser`.

**NotificationIT — mentionNotification (creation):**
- `createPost_createsMentionNotification`, `updatePost_createsNewMentionNotifications`,
  `createQuote_createsMentionNotification`, `createReply_createsMentionNotification`.

**NotificationIT — mentionNotification (deletion):**
- `updatePost_deletesRemovedMentionNotifications`, `deletePost_deletesMentionNotification`,
  `deleteReply_deletesMentionNotification`.

**ValidationIT:**
- `mentionedUserIdsPresent` tests on `CreatePostInput`, `UpdatePostInput`, `CreateReplyInput`,
  `CreateQuoteInput`.

**Test fixtures:**
- `MentionFixtures.createMention(Post, User)` — builds unsaved mention entity.
- `PostHelpers.seedMention(Post, User, MentionRepository)` — saves mention to DB.
- `PostHelpers.addPostWithMentions(HttpGraphQlTester, List<UUID>)` — creates post via GraphQL
  mutation with mentioned user IDs; returns post ID.
