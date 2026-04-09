package com.xclone.post.repository;

import com.xclone.post.model.entity.Post;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** JPA repository for {@link Post} entities. */
@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
  @Query(
      "select p from Post p where p.status = com.xclone.common.enums.Status.ACTIVE"
          + " and p.author.id <> :userId"
          + " and p.author.id in :followingIds order by p.createdAt desc, p.id asc")
  Slice<Post> findFirstPageOfFeed(
      @Param("userId") UUID userId,
      @Param("followingIds") List<UUID> followingIds,
      Pageable pageable);

  @Query(
      "select p from Post p where p.status = com.xclone.common.enums.Status.ACTIVE"
          + " and p.author.id <> :userId"
          + " and p.author.id in :followingIds"
          + " and ((p.createdAt < :cursorCreatedAt)"
          + " or (p.createdAt = :cursorCreatedAt and p.id > :cursorId))"
          + " order by p.createdAt desc, p.id asc")
  Slice<Post> findNextPageOfFeed(
      @Param("userId") UUID userId,
      @Param("followingIds") List<UUID> followingIds,
      @Param("cursorId") UUID cursorId,
      @Param("cursorCreatedAt") Instant createdAt,
      Pageable pageable);
}
