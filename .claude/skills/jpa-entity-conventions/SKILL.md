---
name: jpa-entity-conventions
description: JPA entity design conventions for the X-Clone backend — FK-by-UUID pattern (@ManyToOne + @Column), constraint naming ({domain}_constraint_{name}), @Enumerated(EnumType.STRING), surrogate UUID PKs over composite keys, named FK constraints via @ForeignKey, boolean primitives for NOT NULL columns, FK-only entity references, schema.sql for partial indexes, @Transactional semantics, and JPQL single-entity DELETE limitations.
---

# JPA Entity Conventions

## FK-by-UUID pattern

When a foreign key relationship exists but code only needs the UUID (e.g. setting FK without
loading the target entity):

- `@Column(name = "target_id")` on a `UUID` field — for reading/setting the FK value directly.
- `@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn(name = "target_id", insertable = false, updatable = false)` on the entity reference field — so Hibernate generates the FK constraint.
- `@ForeignKey(name = XConstraintName.TARGET_FK)` on the `@JoinColumn` — meaningful constraint
  names in error messages (Hibernate auto-generates opaque hashes otherwise).
- `@ManyToOne` always annotates a field of **entity type**, never a `UUID`. `@Column` maps
  plain column values.

**`@JoinColumn` `name` vs `referencedColumnName`:**
- `name` = column in the *current* entity's table holding the FK value.
- `referencedColumnName` = column in the *target* entity's table being pointed to (defaults to
  target PK, rarely needed).
- Using `referencedColumnName` when `name` is intended causes Hibernate to mismap the
  relationship.

## FK-only entity reference

When a FK exists in the database but the entity reference is never used in application code
(e.g. `Notification.recipient` — notifications are queried by authenticated user's UUID):
- The `@ManyToOne` field is still added so Hibernate generates the FK constraint.
- Documented with Javadoc explaining it exists solely for constraint generation.
- Keeps FK management consistent across all entities rather than splitting some into
  `schema.sql`.

## Constraint naming

Every constraint name constant uses `{domain}_constraint_{name}`:
- `post_constraint_author_id_fk`, `follow_constraint_self_follow`,
  `notification_constraint_actor_user_id_fk`, etc.
- Lowercase to match PostgreSQL's reported constraint names.
- Constants classes: `PostConstraintName`, `FollowConstraintName`, `LikeConstraintName`,
  `NotificationConstraintName`.

See [references/constraint-naming-rationale.md](references/constraint-naming-rationale.md)
for the full rationale on why `_constraint_` was chosen over bare prefixes.

## Enumerated types

- **`@Enumerated(EnumType.STRING)` on every enum field** — without this, JPA defaults to
  `EnumType.ORDINAL` which stores enums as integer indices. Ordinal storage breaks silently
  when enum values are reordered or inserted. `EnumType.STRING` stores the name and matches
  PostgreSQL's string-based enum types.

## Primary keys

- **Surrogate UUID PK** (`@Id @GeneratedValue(strategy = GenerationType.UUID)`) even when a
  natural composite key exists (e.g. `(actor_user_id, notification_id)` on `NotificationActor`,
  `(follower_id, following_id)` on `Follow`).
- Composite keys in JPA require `@IdClass` or `@EmbeddedId` with a separate `Serializable`
  class implementing `equals`/`hashCode` — significant boilerplate.
- Surrogate PK + unique constraint on the natural key is simpler; business rule still enforced.
- DB schema updated to match (`id UUID PRIMARY KEY DEFAULT gen_random_uuid()`).

## Primitive types for non-nullable columns

- `boolean` (primitive) for `NOT NULL DEFAULT FALSE` columns (e.g. `Notification.read`).
- `Boolean` (boxed) allows null, which is inappropriate. Primitive defaults to `false`,
  matching the database default. Avoids accidental NPE from unboxing.

## Separate entities over @OneToMany collections

When adding a related row is a frequent hot-path operation (e.g. adding a notification actor):
- **Do not use `@OneToMany` collections** — adding to a lazy collection forces Hibernate to
  load the entire collection first. Expensive for viral content.
- **Use separate entities with FK-by-UUID** — adding is a single INSERT + UPDATE on the
  parent's `updatedAt`. One extra line in the service to update `updatedAt`, but write
  efficiency is preserved.
- The `Notification` + `NotificationActor` pattern uses this approach (no
  `List<NotificationActor>` on `Notification`).

## schema.sql for constraints JPA cannot express

- **Partial unique indexes** (e.g. `one_repost_per_user ON posts(shared_post_id, author_id) WHERE message_content IS NULL AND status = 'ACTIVE'`) — JPA annotations cannot express these.
- Managed via `src/main/resources/db/schema.sql`.
- `spring.jpa.defer-datasource-initialization: true` in `application-dev.yml` ensures
  `schema.sql` runs after Hibernate's `ddl-auto: update` creates the tables.
- This approach works for development; a migration tool (Flyway/Liquibase) should replace it
  for production.

## @Transactional semantics

- `@Transactional` is a boundary, not deferred execution — statements execute in call order
  and the transaction sees its own writes; only the commit is deferred to method end.
- The default `AUTO` flush mode flushes pending changes before a query that touches the same
  table — no manual flush needed for read-after-write within a transaction.
- `saveAndFlush()` used when an immediate flush is needed to force constraint checks
  (e.g. `followUser` catching `DataIntegrityViolationException`).
- `@LastModifiedDate` on `updatedAt` ensures reactivated entities get a fresh timestamp via
  dirty checking.

## JPQL DELETE limitations

- JPQL `DELETE` operates on a single entity — no joins in the `FROM` clause.
- Multi-step removal is orchestrated in the service (find → delete → count → conditionally
  delete parent), not via joined bulk delete.
- The FK-by-UUID pattern means FK columns are plain `@Column` values, so no relationship
  traversal is needed — derived delete methods filter columns of a single table.
