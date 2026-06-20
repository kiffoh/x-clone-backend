# Constraint Naming Rationale — `{domain}_constraint_{name}`

## The problem

PostgreSQL constraint names are global within a schema, so unprefixed names like `post_id_fk`
or `like_exists` could collide across tables.

## Alternatives considered

### Bare table/domain prefix

A bare prefix produces `notification_notification_id_fk` for the FK to the notifications
table — correct but reads like a copy-paste error.

### Prefixing by owning table

Prefixing by the owning table instead of the domain does not fix the problem — it only
relocates the doubling. The actor FK on `notification_actors` would become
`notification_actor_actor_user_id_fk`.

## Chosen convention: `{domain}_constraint_{name}`

The `_constraint_` separator sidesteps every repetition case at once and keeps the convention
identical in every class. It is redundant signal next to the `_fk` / `_exists` suffixes, but
that redundancy is the price of uniformity, and the tradeoff was accepted deliberately.

## Applied constants

| Class | Old | New |
|---|---|---|
| `FollowConstraintName` | `follow_exists` | `follow_constraint_follow_exists` |
| | `self_follow` | `follow_constraint_self_follow` |
| | `follower_fk` | `follow_constraint_follower_fk` |
| | `following_fk` | `follow_constraint_following_fk` |
| `LikeConstraintName` | `like_exists` | `like_constraint_like_exists` |
| | `like_post_fk` | `like_constraint_like_post_fk` |
| `NotificationConstraintName` | `actor_user_id_fk` | `notification_constraint_actor_user_id_fk` |
| | `notification_id_fk` | `notification_constraint_notification_id_fk` |
| | `recipient_user_id_fk` | `notification_constraint_recipient_user_id_fk` |
| `PostConstraintName` | `author_id_fk` | `post_constraint_author_id_fk` |
| | `post_parent_fk` | `post_constraint_post_parent_fk` |
| | `shared_post_fk` | `post_constraint_shared_post_fk` |

## Cleanup

Dead `POST_ID_FK` constant and `// Is this clear enough` comment removed from
`NotificationConstraintName` — no `@ManyToOne` for post exists on the `Notification` entity.
