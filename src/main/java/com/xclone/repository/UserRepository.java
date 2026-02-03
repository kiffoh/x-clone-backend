package com.xclone.repository;

import com.xclone.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository to connect User entity to JPA.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
}
