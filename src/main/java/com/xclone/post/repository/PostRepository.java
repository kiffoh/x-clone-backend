package com.xclone.post.repository;

import com.xclone.post.model.entity.Post;
import java.time.Instant;
import java.util.List;
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
   * @param userId excluded from results even if present in followingIds
   * @param followingIds IDs of users whose posts are included in the feed
   * @param pageable page size for the query
   */
  @Query(
      "select p from Post p join p.author a"
          + " where a.status = com.xclone.user.model.enums.UserStatus.ACTIVE"
          + " and p.status = com.xclone.common.enums.Status.ACTIVE"
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
}
