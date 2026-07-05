# X-Clone Backend

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

The project has 22 test files across unit tests, slice tests, and integration tests (Testcontainers).

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
