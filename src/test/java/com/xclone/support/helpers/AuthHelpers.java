package com.xclone.support.helpers;

import com.xclone.security.jwt.JwtTokenProvider;
import com.xclone.user.model.enums.UserRole;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class AuthHelpers {
  @Autowired private JwtTokenProvider jwtTokenProvider;

  public String getUserAccessToken(String userId) {
    return this.jwtTokenProvider.createToken(userId, UserRole.USER.toString());
  }

  public String createExpiredAccessToken(String userId, String role, Date expired) {
    return this.jwtTokenProvider.createToken(userId, role, expired, expired);
  }
}
