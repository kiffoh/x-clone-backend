# Mention — Post Mentions (In Progress)

Phase 17 (Notification Slice 3). Read-side `@BatchMapping` scaffolded; write-side and
notification triggers not yet implemented.

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

## MentionService

- `getPostMentions(List<UUID> postIds)` — groups flat `PostMention` rows into
  `Map<UUID, List<UserProfile>>` via `Collectors.groupingBy` + `Collectors.mapping`.
- Returns `Map<UUID, List<UserProfile>>` — same shape as
  `NotificationService.getMostRecentNotificationActors`.

## PostController integration

- `@BatchMapping(typeName = "Post", field = "mentions")` — batch-resolves mentions for all
  posts; `getOrDefault(post.id(), List.of())` for posts with no mentions.
- Schema: `mentions: [User!]!` — non-null list (empty for posts with no mentions).

## Schema

- `mentions: [User!]!` on `Post` type.
- `mentionedUserIds: [ID!]` on `CreatePostInput`, `UpdatePostInput`, `CreateReplyInput`,
  `CreateQuoteInput` — stubbed, not yet wired.

## Not yet implemented

- `MentionConstraintName` — FK constraint name constants.
- `@ManyToOne` entity references on `Mention` (User, Post) with named FK constraints.
- `@handle` parsing from `messageContent`.
- Write-side: creating mention rows on post/reply/quote creation and update.
- `MENTION` notification trigger via `notificationService.upsertNotification`.
- Integration tests for write-side mutations (`MentionIT` scaffolded, extends
  `BaseGraphQLIntegrationTest`).
