package com.xclone.security.user;

import com.xclone.security.jwt.JwtAuthenticationFilter;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads user data from the database and adapts it to Spring Security's
 * {@link UserDetails} interface via {@link CustomUserDetails}.
 * This service does NOT implement Spring Security's {@link UserDetailsService} interface; instead,
 * custom JWT-based authentication is utilised. {@link JwtAuthenticationFilter} calls
 * {@link #getUserById(String)} directly to authenticate requests based on JWT tokens.
 */
@Service
public class JwtUserDetailsService {
  private final UserRepository userRepository;

  JwtUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * userId is a UUID string.
   */
  public CustomUserDetails getUserById(String userId) {
    try {
      User user = this.userRepository.findById(UUID.fromString(userId)).orElseThrow();
      return new CustomUserDetails(user);
    } catch (Exception e) {
      throw new UsernameNotFoundException(e.getMessage());
    }
  }
}
