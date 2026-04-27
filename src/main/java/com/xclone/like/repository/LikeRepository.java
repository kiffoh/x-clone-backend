package com.xclone.like.repository;

import com.xclone.like.dto.LikeCount;
import com.xclone.like.model.entity.Like;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** JPA repository for {@link Like} entities. */
@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

  @Query(
      "select new com.xclone.like.dto.LikeCount(l.postId, count(l)) from Like l join l.user u"
          + " where u.status = com.xclone.user.model.enums.UserStatus.ACTIVE"
          + " and l.postId in :postIds"
          + " group by l.postId")
  List<LikeCount> findActiveLikesByPostIds(@Param("postIds") List<UUID> postIds);

  @Query("select l.postId from Like l where l.userId = :userId and l.postId in :postIds")
  List<UUID> findPostIdsThatUserLikes(
      @Param("postIds") List<UUID> postIds, @Param("userId") UUID userId);

  @Query(
      "select l from Like l join fetch l.user u"
          + " where u.status = com.xclone.user.model.enums.UserStatus.ACTIVE"
          + " and l.postId = :postId"
          + " order by l.createdAt desc, l.id asc")
  Slice<Like> findFirstPageOfUsersThatLikedPost(@Param("postId") UUID postId, Pageable pageable);

  @Query(
      "select l from Like l join fetch l.user u"
          + " where u.status = com.xclone.user.model.enums.UserStatus.ACTIVE"
          + " and l.postId = :postId"
          + " and ((l.createdAt < :cursorCreatedAt)"
          + " or (l.createdAt = :cursorCreatedAt and l.id > :cursorId))"
          + " order by l.createdAt desc, l.id asc")
  Slice<Like> findNextPageOfUsersThatLikedPost(
      @Param("postId") UUID postId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      @Param("cursorId") UUID cursorId,
      Pageable pageable);

  void deleteLikeByPostIdAndUserId(UUID postId, UUID userId);
}
