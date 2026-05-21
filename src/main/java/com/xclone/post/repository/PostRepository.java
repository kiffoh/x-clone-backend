package com.xclone.post.repository;

import com.xclone.post.model.entity.Post;
import com.xclone.reply.dto.ReplyCount;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** JPA repository for {@link Post} entities. */
@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
  /**
   * Gets first page of feed.
   *
   * <p>A null parent id corresponds to the first post in that post thread. TODO: Update comment
   * when quotes are introduced.
   *
   * @param userId excluded from results even if present in followingIds
   * @param followingIds IDs of users whose posts are included in the feed
   * @param pageable page size for the query
   */
  @Query(
      "select p from Post p join p.author a"
          + " where a.status = com.xclone.user.model.enums.UserStatus.ACTIVE"
          + " and p.status = com.xclone.common.enums.Status.ACTIVE"
          + " and p.parentId is null"
          + " and p.authorId <> :userId"
          + " and p.authorId in :followingIds"
          + " order by p.createdAt desc, p.id asc")
  Slice<Post> findFirstPageOfFeed(
      @Param("userId") UUID userId,
      @Param("followingIds") List<UUID> followingIds,
      Pageable pageable);

  /**
   * Gets next page of feed after the cursor.
   *
   * <p>A null parent id corresponds to the first post in that post thread. TODO: Update comment
   * when quotes are introduced.
   *
   * @param userId excluded from results even if present in followingIds
   * @param followingIds IDs of users whose posts are included in the feed
   * @param cursorId id to query after
   * @param cursorCreatedAt datetime to query after
   * @param pageable page size for the query
   */
  @Query(
      "select p from Post p join p.author a"
          + " where a.status = com.xclone.user.model.enums.UserStatus.ACTIVE"
          + " and p.status = com.xclone.common.enums.Status.ACTIVE"
          + " and p.parentId is null"
          + " and p.authorId <> :userId"
          + " and p.authorId in :followingIds"
          + " and ((p.createdAt < :cursorCreatedAt)"
          + " or (p.createdAt = :cursorCreatedAt and p.id > :cursorId))"
          + " order by p.createdAt desc, p.id asc")
  Slice<Post> findNextPageOfFeed(
      @Param("userId") UUID userId,
      @Param("followingIds") List<UUID> followingIds,
      @Param("cursorId") UUID cursorId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      Pageable pageable);

  @Query(
      "select p from Post p where p.authorId = :userId"
          + " and p.status = com.xclone.common.enums.Status.ACTIVE"
          + " order by p.createdAt desc, p.id asc")
  Slice<Post> findFirstPageOfUsersPosts(@Param("userId") UUID userId, Pageable pageable);

  @Query(
      "select p from Post p where p.authorId = :userId"
          + " and p.status = com.xclone.common.enums.Status.ACTIVE"
          + " and ((p.createdAt < :cursorCreatedAt)"
          + " or (p.createdAt = :cursorCreatedAt and p.id > :cursorId))"
          + " order by p.createdAt desc, p.id asc")
  Slice<Post> findNextPageOfUsersPosts(
      @Param("userId") UUID userId,
      @Param("cursorId") UUID cursorId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      Pageable pageable);

  @Modifying
  @Query(
      "update Post p set p.status = com.xclone.common.enums.Status.DELETED"
          + " where p.authorId = :userId")
  void softDeleteAllByUserId(@Param("userId") UUID userId);

  List<Post> findAllByAuthorId(UUID userId);

  @Query(
      "select new com.xclone.reply.dto.ReplyCount(p.parentId, count(p)) from Post p"
          + " where p.status = com.xclone.common.enums.Status.ACTIVE"
          + " and p.parentId in :parentIds group by p.parentId")
  List<ReplyCount> findAllReplyCountsByParentIds(@Param("parentIds") List<UUID> postIds);

  @Query(
      "select p from Post p where p.id = :postId"
          + " and p.status = com.xclone.common.enums.Status.ACTIVE")
  Optional<Post> findActivePostById(@Param("postId") UUID postId);

  /**
   * Fetches a repost.
   *
   * <p>Method is agnostic of post status to allow for repost activation.
   *
   * @param postId unique identifier of the original post id
   * @param authorId unique identifier of the post author
   * @return post entity or null
   */
  @Query(
      "select p from Post p where p.quotedPostId = :quotedPostId and p.authorId = :authorId "
          + "and p.messageContent is null")
  Optional<Post> findRepost(@Param("quotedPostId") UUID postId, @Param("authorId") UUID authorId);

  @Query(
      "select p from Post p where p.parentId = :parentId"
          + " and p.status = com.xclone.common.enums.Status.ACTIVE"
          + " order by p.createdAt desc, p.id asc")
  Slice<Post> findFirstPageOfReplies(@Param("parentId") UUID postId, Pageable pageable);

  @Query(
      "select p from Post p where p.parentId = :parentId"
          + " and p.status = com.xclone.common.enums.Status.ACTIVE"
          + " and ((p.createdAt < :cursorCreatedAt)"
          + " or (p.createdAt = :cursorCreatedAt and p.id > :cursorId))"
          + " order by p.createdAt desc, p.id asc")
  Slice<Post> findNextPageOfReplies(
      @Param("parentId") UUID postId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      @Param("cursorId") UUID cursorId,
      Pageable pageable);

  /**
   * Fetches the original posts for each quote.
   *
   * @param quotedPostIds list of ids for each quote entity
   * @return list of quoted posts
   */
  @Query(
      "select p from Post p where p.id in :quotedPostIds "
          + " and p.status = com.xclone.common.enums.Status.ACTIVE")
  List<Post> findQuotedPosts(@Param("quotedPostIds") List<UUID> quotedPostIds);

  @Query(
      "select p from Post p where p.quotedPostId = :quotedPostId"
          + " and p.status = com.xclone.common.enums.Status.ACTIVE"
          + " order by p.createdAt desc, p.id asc")
  Slice<Post> findFirstPageOfQuotes(@Param("quotedPostId") UUID quotedPostId, Pageable pageable);

  @Query(
      "select p from Post p where p.quotedPostId = :quotedPostId"
          + " and p.status = com.xclone.common.enums.Status.ACTIVE"
          + " and ((p.createdAt < :cursorCreatedAt)"
          + " or (p.createdAt = :cursorCreatedAt and p.id > :cursorId))"
          + " order by p.createdAt desc, p.id asc")
  Slice<Post> findNextPageOfQuotes(
      @Param("quotedPostId") UUID quotedPostId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      @Param("cursorId") UUID cursorId,
      Pageable pageable);

  /**
   * Fetches older posts in the reply chain.
   *
   * <p>Posts with a deleted status are included in the returned value.
   *
   * <p>The queried post is excluded in the returned value.
   *
   * @param postId unique identifier of the queried post
   * @return list of posts sorted by created date ascendingly
   */
  @Query(
      value =
          "WITH RECURSIVE post_tree AS ( "
              + " SELECT * FROM posts WHERE id = :postId UNION ALL"
              + " SELECT p.* FROM posts p JOIN post_tree pt ON p.id = pt.parent_id)"
              + " SELECT * FROM post_tree WHERE id != :postId ORDER BY created_at ASC",
      nativeQuery = true)
  List<Post> findAllAncestors(@Param("postId") UUID postId);

  /**
   * Fetches older siblings that are active replies.
   *
   * @param parentId unique identifier of the shared parent post
   * @param postCreatedAt datetime of when the queried post was created
   * @return list of active posts sorted by created date ascendingly
   */
  @Query(
      "select p from Post p where p.parentId = :parentId and p.createdAt < :postCreatedAt"
          + " and p.status = com.xclone.common.enums.Status.ACTIVE order by p.createdAt asc")
  List<Post> findAllSiblings(
      @Param("parentId") UUID parentId, @Param("postCreatedAt") Instant postCreatedAt);
}
