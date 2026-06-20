# Like — Likes & Like Counts

## Like entity

- `Like` JPA entity in `like/model/entity/`.
- `LikeConstraintName` — uses `{domain}_constraint_{name}` convention:
  `like_constraint_like_exists`, `like_constraint_like_post_fk`.

## LikeService

- `createLike` — `saveAndFlush()` for immediate constraint check; catches
  `DataIntegrityViolationException` translated to typed exceptions; null-safety guards on
  `getRootCause()` → `getServerErrorMessage()` → `getConstraint()` chain (SpotBugs pattern).

## LikeController

- `LikeController` lives in `like/controller/`.

## NotPostAuthorException

- Thrown when unauthorised access to `likes` query.
- Handled in `GraphQlExceptionHandler` for queries — returns `FORBIDDEN`.
- Mutations (`updatePostContent`, `deletePost`) catch explicitly before it reaches the handler.

## LikeCount record

- Lives in `like/dto/LikeCount.java`.

## Testing

- `LikeIT` — integration tests in `integration/like/`.
- `LikeFixtures`, `LikeHelpers` in test support.
