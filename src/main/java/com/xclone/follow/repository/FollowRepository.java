package com.xclone.follow.repository;

import com.xclone.follow.model.entity.Follow;
import com.xclone.user.model.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Connects the Follow entity to the JPA. */
@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

  Integer countByFollower_Id(UUID followerId);

  List<Follow> findAllByFollower_IdAndFollowing_IdIn(UUID followerId, List<UUID> followingIds);

  Slice<Follow> findAllByFollower_IdOrderByCreatedAtDescIdAsc(UUID followerId, Pageable pageable);

  @Query("select f.following.id from Follow f where f.follower.id = :followerId")
  List<UUID> findFollowingIdsByFollowerId(@Param("followerId") UUID followingId);

  @Query(
      "select f from Follow f where f.follower.id = :followerId and "
          + "((f.createdAt < :cursorCreatedAt) "
          + "or (f.createdAt = :cursorCreatedAt and f.id > :cursorId))"
          + " order by f.createdAt desc, f.id asc")
  Slice<Follow> findFollowerNextPage(
      @Param("followerId") UUID followerId,
      @Param("cursorId") UUID cursorId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      Pageable pageable);

  Integer countByFollowing_Id(UUID followingId);

  Slice<Follow> findAllByFollowing_IdOrderByCreatedAtDescIdAsc(UUID followingId, Pageable pageable);

  @Query(
      "select f from Follow f where f.following.id = :followingId and "
          + "((f.createdAt < :cursorCreatedAt) "
          + "or (f.createdAt = :cursorCreatedAt and f.id > :cursorId))"
          + " order by f.createdAt desc, f.id asc")
  Slice<Follow> findFollowingNextPage(
      @Param("followingId") UUID followingId,
      @Param("cursorId") UUID cursorId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      Pageable pageable);

  void deleteByFollowerAndFollowing(User follower, User following);
}
