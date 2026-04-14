package com.xclone.follow.repository;

import com.xclone.follow.model.entity.Follow;
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

  @Query(
      "select f.following.id from Follow f"
          + " where f.follower.id = :followerId and f.following.id in :userIds")
  List<UUID> findFollowingIdsInList(
      @Param("followerId") UUID followerId, @Param("userIds") List<UUID> userIds);

  @Query(
      "select f from Follow f where f.follower.id = :followerId "
          + " and f.following.status = com.xclone.user.model.enums.UserStatus.ACTIVE"
          + " order by f.createdAt desc, f.id asc")
  Slice<Follow> findFirstPageOfFollowing(@Param("followerId") UUID followerId, Pageable pageable);

  @Query(
      "select f from Follow f where f.follower.id = :followerId "
          + " and f.following.status = com.xclone.user.model.enums.UserStatus.ACTIVE"
          + " and ((f.createdAt < :cursorCreatedAt) "
          + "or (f.createdAt = :cursorCreatedAt and f.id > :cursorId))"
          + " order by f.createdAt desc, f.id asc")
  Slice<Follow> findNextPageOfFollowing(
      @Param("followerId") UUID followerId,
      @Param("cursorId") UUID cursorId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      Pageable pageable);

  @Query("select f.following.id from Follow f where f.follower.id = :followerId")
  List<UUID> findFollowingIdsByFollowerId(@Param("followerId") UUID followerId);

  Integer countByFollowing_Id(UUID followingId);

  @Query(
      "select f from Follow f where f.following.id = :followingId"
          + " and f.follower.status = com.xclone.user.model.enums.UserStatus.ACTIVE"
          + " order by f.createdAt desc, f.id asc")
  Slice<Follow> findFirstPageOfFollowers(@Param("followingId") UUID followingId, Pageable pageable);

  @Query(
      "select f from Follow f where f.following.id = :followingId"
          + " and f.follower.status = com.xclone.user.model.enums.UserStatus.ACTIVE"
          + " and ((f.createdAt < :cursorCreatedAt) "
          + "or (f.createdAt = :cursorCreatedAt and f.id > :cursorId))"
          + " order by f.createdAt desc, f.id asc")
  Slice<Follow> findNextPageOfFollowers(
      @Param("followingId") UUID followingId,
      @Param("cursorId") UUID cursorId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      Pageable pageable);

  void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
}
