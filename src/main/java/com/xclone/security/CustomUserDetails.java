package com.xclone.security;

import com.xclone.model.entity.User;
import com.xclone.model.enums.UserRole;
import com.xclone.model.enums.UserStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Maps User entity to UserDetails. */
public class CustomUserDetails implements UserDetails {
  private final User user;

  public CustomUserDetails(User user) {
    this.user = user;
  }

  @Override
  public String getPassword() {
    return user.getPasswordHash();
  }

  @Override
  public String getUsername() {
    return user.getId().toString();
  }

  @Override
  public boolean isAccountNonExpired() {
    return user.getStatus() == UserStatus.ACTIVE;
  }

  @Override
  public boolean isAccountNonLocked() {
    return user.getStatus() != UserStatus.SUSPENDED;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    if (this.user.getRole() == UserRole.ADMIN) {
      return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
    return List.of(new SimpleGrantedAuthority("ROLE_USER"));
  }
}
