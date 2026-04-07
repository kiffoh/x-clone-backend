package com.xclone.user.repository;

import com.xclone.user.model.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository to connect User entity to JPA. */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByHandle(String handle);

  Slice<User> findAllByHandleContainingOrderByCreatedAtDescIdAsc(String query, Pageable pageable);

  @Query(
      "select u from User u where u.handle LIKE %:query% and "
          + "((u.createdAt < :cursorCreatedAt) "
          + "or (u.createdAt = :cursorCreatedAt and u.id > :cursorId))"
          + " order by u.createdAt desc, u.id asc")
  Slice<User> findAllByHandleContainingNextPage(
      @Param("query") String query,
      @Param("cursorId") UUID cursorId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      Pageable pageable);

  Slice<User> findAllByIdNotIn(List<UUID> userIds, Pageable pageable);

  @Query(
      "select u from User u where u.id NOT IN :userIds and "
          + "((u.createdAt < :cursorCreatedAt) "
          + "or (u.createdAt = :cursorCreatedAt and u.id > :cursorId))"
          + " order by u.createdAt desc, u.id asc")
  Slice<User> findAllByIdNotInNext(
      @Param("userIds") List<UUID> userIds,
      @Param("cursorId") UUID cursorId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      Pageable pageable);

  boolean existsByHandle(String handle);

  boolean existsByHandleAndIdNot(String handle, UUID id);
}
