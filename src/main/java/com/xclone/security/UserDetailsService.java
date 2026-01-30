package com.xclone.security;

import com.xclone.model.entity.User;
import com.xclone.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Load user from DB for authentication */
@Service
public class UserDetailsService {
  private final UserRepository userRepository;

  UserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /** userId is a UUID string */
  public CustomUserDetails getUserById(String userId) {
    try {
      User user = this.userRepository.findById(userId).orElseThrow();
      return new CustomUserDetails(user);
    } catch (Exception e) {
      throw new UsernameNotFoundException(e.getMessage());
    }
  }
}
