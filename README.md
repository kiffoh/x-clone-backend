# X-Clone Backend

[![Backend CI](https://github.com/kiffoh/x-clone-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/kiffoh/x-clone-backend/actions/workflows/ci.yml)

A social media platform backend inspired by X/Twitter, built with Spring Boot and GraphQL.

## Tech Stack

- **Java 21** + **Spring Boot 3.5**
- **GraphQL** (Spring for GraphQL) — schema-first API design
- **Spring Security** + **JWT** — stateless authentication with refresh tokens
- **Spring Data JPA** + **PostgreSQL 16** — relational persistence
- **Redis 7** — token blocklisting and caching
- **Testcontainers** — integration tests against real Postgres/Redis
- **Lombok**, **SpotBugs**, **Checkstyle**, **JaCoCo**

## Features

- **Authentication** — register, login, refresh tokens, logout (REST + Swagger UI)
- **Users** — profiles, search, suggested users, account deletion (soft delete)
- **Posts** — create, update, delete, feed (from followed accounts)
- **Replies** — threaded replies with recursive reply-thread view
- **Shares** — reposts and quotes with share counts
- **Likes** — like/unlike with per-post counts
- **Follows** — follow/unfollow with paginated follower/following lists
- **Notifications** — grouped notifications with actor previews (like, reply, repost, quote, follow, mention)
- **Mentions** — tag users in posts, batch-resolved via `@BatchMapping`
- **Cursor-based pagination** — Relay-style keyset pagination across all list endpoints

## Architecture

```
src/main/java/com/xclone/
├── auth/           # JWT authentication (REST)
├── user/           # User queries + mutations
├── post/           # Posts, feed, reply threads
├── reply/          # Reply creation + thread queries
├── share/          # Reposts + quotes
├── like/           # Like/unlike
├── follow/         # Follow graph
├── notification/   # Notification grouping + triggers
├── mention/        # Post mentions
├── common/         # Shared enums, pagination, connections
├── security/       # Security config, JWT filters
├── exception/      # REST + GraphQL error handling
└── validation/     # Custom validators (@ValidHandle, @ValidPassword)
```

Each feature is a **vertical slice** with explicit `Controller → Service → Repository` layering. JPA entities are never exposed in API responses — each entity projects to an immutable `record` via a `toXProfile()` method.

## Design decisions

### Keyset (cursor) pagination instead of offset

Every paginated list (feed, replies, followers/following, quotes, reposts, search, notifications) uses keyset pagination rather than `OFFSET`/`LIMIT`. Each repository exposes a `findFirstPageOfX` / `findNextPageOfX` pair; the next-page query filters on `(createdAt < :cursorTimestamp) OR (createdAt = :cursorTimestamp AND id > :cursorId)`, ordered by `createdAt desc, id asc` (notifications order on `updatedAt` instead, so items with new activity bubble to the top). The cursor itself is a `timestamp_id` pair, base64-encoded in `Cursor.encode()`/`Cursor.toCursor()` (`src/main/java/com/xclone/common/connection/Cursor.java`). Keyset avoids the performance cliff of `OFFSET` on large tables and stays stable when rows are inserted or deleted between page requests, which offset pagination does not.

### `@BatchMapping` resolvers to avoid N+1 queries

Twelve `@BatchMapping` resolvers batch-load associated data for a page of results in one query instead of once per row: eight on `Post` (`author`, `likeCount`, `likedByMe`, `replyCount`, `sharedPost`, `shareCount`, `sharedByMe`, `mentions`, in `src/main/java/com/xclone/post/controller/PostController.java`), three on `Notification` (`post`, `actors`, `actorCount`, in `src/main/java/com/xclone/notification/controller/NotificationController.java`), and one on `User` (`isFollowing`, in `src/main/java/com/xclone/user/controller/UserController.java`).

### Recursive CTE for reply-thread ancestors

`PostRepository.findAllAncestors` (`src/main/java/com/xclone/post/repository/PostRepository.java`) uses a native `WITH RECURSIVE post_tree AS (...)` query that walks up from a post through `parent_id` to the root, returning the full ancestor chain in one round trip. `ReplyService.getReplyThread` combines this with a sibling lookup (`findAllSiblings`) to build the `ReplyThread` (ancestors, siblings, focused post) shown when a reply is opened.

### Partial unique indexes for notification and repost invariants

`src/main/resources/db/schema.sql` defines three partial unique indexes: `one_like_notification_per_recipient` and `one_repost_notification_per_recipient` on `notifications(post_id, recipient_user_id)` filtered by `type`, and `one_repost_per_user` on `posts(shared_post_id, author_id)` filtered to active pure reposts. These enforce "at most one aggregate notification per post/recipient/type" and "at most one active repost per user per post" at the database level rather than relying solely on application checks.

Under PostgreSQL's default READ COMMITTED isolation (no isolation override is set in the codebase), two concurrent requests can both read "no existing notification" and both attempt an insert. `NotificationService.upsertNotification` (`src/main/java/com/xclone/notification/service/NotificationService.java`) handles this by catching the resulting `DataIntegrityViolationException`, inspecting the underlying `PSQLException`'s constraint name via `NotificationConstraintName`, and swallowing the exception when it matches one of the two aggregate-notification constraints, treating the loser of the race as a no-op rather than an error.

## Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven (or use the included `./mvnw` wrapper)

## Getting Started

1. **Start infrastructure:**

   ```bash
   docker compose up -d
   ```

   This starts PostgreSQL 16 and Redis 7.

2. **Set environment variables:**

   ```bash
   cp .env.sample .env
   source .env
   ```

3. **Run the application:**

   ```bash
   ./mvnw spring-boot:run
   ```

4. **Access the API:**

   - GraphQL endpoint: `http://localhost:8080/graphql`
   - Swagger UI (auth endpoints): `http://localhost:8080/swagger-ui.html`

## Testing

The project has 20 test files across unit tests, slice tests, and integration tests (Testcontainers).

```bash
# Run all tests
./mvnw verify

# Run only unit tests
./mvnw test

# Run only integration tests
./mvnw failsafe:integration-test failsafe:verify
```

## API Overview

### Authentication (REST)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/register` | POST | Create a new account |
| `/api/auth/login` | POST | Authenticate and receive tokens |
| `/api/auth/refresh` | POST | Refresh an expired access token |
| `/api/auth/logout` | POST | Invalidate refresh token |

### GraphQL

**Queries:** `me`, `userByHandle`, `userById`, `searchUsers`, `suggestedUsers`, `getPost`, `feed`, `getReplyThread`, `getNotifications`

**Mutations:** `updateMyProfile`, `deleteMyAccount`, `followUser`, `unfollowUser`, `createPost`, `createReply`, `createRepost`, `createQuote`, `updatePostContent`, `deletePost`, `likePost`, `unlikePost`, `readNotification`

All GraphQL operations require authentication via Bearer token.

## Project Status

This project is under active development. See [STATUS.md](STATUS.md) for current progress and upcoming work.
