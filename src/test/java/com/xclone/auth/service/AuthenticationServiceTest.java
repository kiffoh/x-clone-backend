package com.xclone.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xclone.auth.dto.AuthTokens;
import com.xclone.auth.dto.LoginRequest;
import com.xclone.auth.dto.SignupRequest;
import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.security.jwt.JwtTokenProvider;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {
  @Mock
  JwtTokenProvider jwtTokenProvider;
  @Mock
  UserRepository userRepository;
  @Mock
  PasswordEncoder passwordEncoder;
  @Mock
  RefreshTokenService refreshTokenService;

  @InjectMocks
  AuthenticationService authenticationService;

  @BeforeEach
  void setUp() {
    ArgumentCaptor<String> refreshTokenCaptor;
    ArgumentCaptor<String> accessTokenCaptor;
  }

  @Test
  void login_createsTokens_returnsAuthTokens() {
    // Setup
    User exampleUser = new User();
    exampleUser.setPasswordHash("hashedPassword");
    exampleUser.setHandle("exampleHandle");
    exampleUser.setId(UUID.randomUUID());
    when(this.userRepository.findByHandle(anyString()))
        .thenReturn(Optional.of(exampleUser));
    when(this.passwordEncoder.matches(anyString(), eq("hashedPassword")))
        .thenReturn(true);

    // Trigger
    LoginRequest req = new LoginRequest("exampleHandle", "password");
    this.authenticationService.login(req);

    // Assert
    verify(userRepository, times(1)).findByHandle(
        exampleUser.getHandle()
    );
    verify(passwordEncoder, times(1)).matches(
        "password",
        "hashedPassword"
    );
    verify(refreshTokenService, times(1)).createToken(
        exampleUser.getId().toString()
    );
  }

  @Test
  void login_attemptForInvalidUser_returnsBadCredentials() {
    when(this.userRepository.findByHandle(anyString())).thenReturn(Optional.empty());

    LoginRequest req = new LoginRequest("exampleHandle", "password");

    assertThatThrownBy(() -> this.authenticationService.login(req))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("Invalid credentials");
  }

  @Test
  void login_attemptWithInvalidPassword_returnsBadCredentials() {
    User exampleUser = new User();
    exampleUser.setPasswordHash("hashedPassword");
    when(this.userRepository.findByHandle(anyString()))
        .thenReturn(Optional.of(exampleUser));
    when(this.passwordEncoder.matches(anyString(), eq("hashedPassword")))
        .thenReturn(false);

    LoginRequest req = new LoginRequest("exampleHandle", "password");

    assertThatThrownBy(() -> this.authenticationService.login(req))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("Invalid credentials");
  }

  @Test
  void signup_createsUser_automaticallyAssignsDisplayName_returnsAuthTokens() {
    // Arrange
    when(this.userRepository.existsByHandle(anyString())).thenReturn(false);
    when(this.userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      user.setId(UUID.randomUUID());
      return user;
    });
    when(passwordEncoder.encode("password")).thenReturn("hashed_password");

    SignupRequest req = new SignupRequest("exampleHandle",
        "password",
        null,
        null,
        null);

    // Act
    AuthTokens res = this.authenticationService.signup(req);

    // Assert
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(this.userRepository).save(userCaptor.capture());
    User capturedUser = userCaptor.getValue();

    assertThat(capturedUser.getHandle()).isEqualTo("exampleHandle");
    assertThat(capturedUser.getDisplayName()).isEqualTo("exampleHandle");
    assertThat(capturedUser.getPasswordHash()).isEqualTo("hashed_password");
    assertThat(capturedUser.getId().toString()).isEqualTo(res.userId());
    assertThat(res.displayName()).isEqualTo("exampleHandle");
  }

  @Test
  void signup_createsUseWithDisplayName_returnsAuthTokens() {
    // Arrange
    when(this.userRepository.existsByHandle(anyString())).thenReturn(false);
    when(this.userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      user.setId(UUID.randomUUID());
      return user;
    });
    when(passwordEncoder.encode("password")).thenReturn("hashed_password");

    SignupRequest req = new SignupRequest("exampleHandle",
        "password",
        "displayName",
        null,
        null);

    // Act
    AuthTokens res = this.authenticationService.signup(req);

    // Assert
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(this.userRepository).save(userCaptor.capture());
    User capturedUser = userCaptor.getValue();

    assertThat(capturedUser.getHandle()).isEqualTo("exampleHandle");
    assertThat(capturedUser.getDisplayName()).isEqualTo("displayName");
    assertThat(capturedUser.getPasswordHash()).isEqualTo("hashed_password");
    assertThat(capturedUser.getId().toString()).isEqualTo(res.userId());
    assertThat(res.displayName()).isEqualTo("displayName");
  }

  @Test
  void signup_attemptWithExistingHandle_returnsDuplicateHandle() {
    // Arrange
    when(this.userRepository.existsByHandle(anyString())).thenReturn(true);

    SignupRequest req = new SignupRequest("exampleHandle",
        "password",
        "displayName",
        null,
        null);

    // Act + Assert
    assertThatThrownBy(() -> this.authenticationService.signup(req))
        .isInstanceOf(DuplicateHandleException.class)
        .hasMessageContaining("This handle is already taken");
  }

}
