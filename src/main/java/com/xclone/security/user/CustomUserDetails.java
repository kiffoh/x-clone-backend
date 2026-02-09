package com.xclone.security.user;

import com.xclone.user.model.entity.User;
import com.xclone.user.model.enums.UserRole;
import com.xclone.user.model.enums.UserStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adapts the {@link User} entity to Spring Security's {@link UserDetails} interface.
 * This allows Spring Security to authenticate users without coupling security
 * concerns to our domain model.
 */
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
