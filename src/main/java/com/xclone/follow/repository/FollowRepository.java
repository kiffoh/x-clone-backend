package com.xclone.follow.repository;

import com.xclone.follow.model.entity.Follow;
import com.xclone.user.model.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Connects the Follow entity to the JPA. */
@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

  List<Follow> findAllByFollower_IdAndFollowing_IdIn(UUID followerId, List<UUID> followingIds);

  Page<Follow> findAllByFollower_IdOrderByCreatedAtDescIdAsc(UUID followerId, Pageable pageable);

  @Query(
      "select f from Follow f where f.follower.id = :followerId and "
          + "((f.createdAt < :cursorCreatedAt) "
          + "or (f.createdAt = :cursorCreatedAt and f.id > :cursorId))"
          + " order by f.createdAt desc, f.id asc")
  Page<Follow> findFollowerNextPage(
      @Param("followerId") UUID followerId,
      @Param("cursorId") UUID cursorId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      Pageable pageable);

  Page<Follow> findAllByFollowing_IdOrderByCreatedAtDescIdAsc(UUID followingId, Pageable pageable);

  @Query(
      "select f from Follow f where f.following.id = :followingId and "
          + "((f.createdAt < :cursorCreatedAt) "
          + "or (f.createdAt = :cursorCreatedAt and f.id > :cursorId))"
          + " order by f.createdAt desc, f.id asc")
  Page<Follow> findFollowingNextPage(
      @Param("followingId") UUID followingId,
      @Param("cursorId") UUID cursorId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      Pageable pageable);

  void deleteByFollowerAndFollowing(User follower, User following);
}
