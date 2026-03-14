package com.xclone.user.repository;

import com.xclone.user.model.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository to connect User entity to JPA. */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByHandle(String handle);

  List<User> findAllByHandleContaining(String query);

  boolean existsByHandle(String handle);

  boolean existsByHandleAndIdNot(String handle, UUID id);
}
