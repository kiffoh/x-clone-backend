package com.xclone.user.contoller;

import com.xclone.security.user.CustomUserDetails;
import com.xclone.support.fixtures.UserFixtures;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

class WithMockCustomUserSecurityContextFactory
    implements WithSecurityContextFactory<WithMockCustomUser> {
  @Override
  public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();

    CustomUserDetails user = new CustomUserDetails(UserFixtures.getDefaultUserWithStaticId());

    Authentication auth =
        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

    context.setAuthentication(auth);
    return context;
  }
}
