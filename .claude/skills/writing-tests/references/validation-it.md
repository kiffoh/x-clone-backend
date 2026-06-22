# ValidationIT Structure

Purpose: owns all **cross-cutting validation constraint tests**. Happy paths are not tested
here — they are implicitly exercised by every feature integration test that passes valid input.

## Nested test classes

| Class | Endpoint | Transport |
|---|---|---|
| `ValidHandleTests` | `POST /api/auth/signup` | `TestRestTemplate` |
| `ValidPasswordTests` | `POST /api/auth/signup` | `TestRestTemplate` |
| `ObjectNotEmptyTests` | `updateMyProfile` mutation | `HttpGraphQlTester` |
| `MalformedCursorTests` | Any paginated endpoint | `HttpGraphQlTester` |
| `CreatePostInputTests` | `createPost` mutation | `HttpGraphQlTester` |
| `UpdatePostInputTests` | `updatePostContent` mutation | `HttpGraphQlTester` |
| `CreateReplyInputTests` | `createReply` mutation | `HttpGraphQlTester` |
| `CreateQuoteInputTests` | `createQuote` mutation | `HttpGraphQlTester` |

## MalformedCursorTests

Cross-cutting concern tested once — all paginated endpoints go through `Cursor.toCursor` →
`InvalidCursorException` → `GraphQlExceptionHandler` — the same code path.

## CreatePostInputTests / UpdatePostInputTests / CreateReplyInputTests / CreateQuoteInputTests

Tests `messageContent` validation (too long, empty, null coercion) and input-specific field
null coercion (e.g. `postId`, `parentId`, `sharedPostId`).

## Setup

- Outer `@BeforeEach` calls `cleanupDBs()` — `postRepository.deleteAll()` and
  `userRepository.deleteAll()`.

## Key lessons

- Jakarta Bean Validation (Hibernate Validator) has no Spring dependency — constraints can be
  unit tested with `Validation.buildDefaultValidatorFactory().getValidator()` directly.
- `@Nested` class instance field initialisers execute at construction time, before Spring
  injection is complete — setup that touches injected beans must go in `@BeforeEach`, not in
  field initialisers.
- `@Nested` classes inherit `@BeforeEach` from the outer class but do not inherit field
  initialisers.
