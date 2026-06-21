# X-Clone review conventions

Living checklist. Prune anything that stops being true; add new recurring findings
as they surface in review. Keep it specific — vague rules don't catch bugs.

## Naming

- Repository methods use the `find` prefix; service methods use the `get` prefix.
- DB constraints follow `{domain}_constraint_{name}`. The `_constraint_` separator
  is deliberate — it avoids word-doubling across all constraint types. Flag any
  constraint that omits it or doubles a word.
- GraphQL schema: `"""` triple-quote descriptions for client-facing fields; `#`
  comments for dev-only notes. Flag client-facing fields documented with `#`.

## JPA / JPQL

- **JPQL property paths must match entity field names exactly.** This is the most
  common recurring bug — transpositions like `na.userActorId` where the entity
  field is `actorUserId`. Cross-check every property path in a `@Query` against
  the referenced entity's actual field names.
- Enum columns use `@Enumerated(EnumType.STRING)` (e.g. `NotificationType`). Flag
  any enum mapped ordinally.
- Explicit FK patterns, UUID surrogate keys — keep consistent with existing entities.

## Null-safety

- `@BatchMapping` is the standard resolver pattern for feed-level N+1 risk.
- FOLLOW-type notifications have a **null `postId`**. Any batch mapping or code path
  that dereferences `postId` must guard for FOLLOW first. This is a recurring gap.

## Notification domain

- Self-notification guard: early return in `upsertNotification` when `recipientId`
  equals the acting user's id. Flag new notification triggers that omit it.
- FOLLOW notifications aggregate over a 12-hour window; QUOTE / REPLY / MENTION are
  discrete. Check that new trigger code respects the right model.
- Notification lookup queries that locate a notification for deletion must join on
  the actor (`na.actorUserId = :actorId`). Without the join, `ORDER BY updatedAt
  DESC LIMIT 1` can return a different user's notification when multiple
  notifications share the same `(recipientId, postId, type)` — the delete is then
  a silent no-op and the correct notification leaks. This was caught for both
  FOLLOW (resolved via `findSpecificFollowNotification`) and discrete types
  (QUOTE/REPLY — resolved by adding actor join to `findNotification`).

## Tests

- AssertJ for most assertions; JUnit `assertTrue`/`assertFalse` accepted in some IT
  classes but prefer AssertJ in new code.
- Integration tests extend `BaseGraphQLIntegrationTest` and rely on FK-safe
  `@BeforeEach` cleanup ordering — new IT setup/teardown must not break that order.
- `*IT` tests run via Failsafe. These were silently skipped before Failsafe was
  wired up, so confirm new IT classes match the `*IT` pattern and actually execute.
- Document intentional test omissions with fully-qualified Javadoc `{@link}`
  references to what's covered elsewhere.

## Architecture patterns (consistency checks)

- Reposts use a soft-delete toggle backed by a PostgreSQL partial unique index
  (one active repost per user per post). Flag hard deletes or duplicate-active paths.
- `quotedPostId` is exposed in the schema so clients can distinguish quotes of
  deleted originals — don't strip it.
- Replying to soft-deleted posts is intentionally allowed (thread continuity).
  Don't "fix" it.