package com.xclone.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xclone.auth.dto.AuthTokens;
import com.xclone.auth.dto.LoginRequest;
import com.xclone.auth.dto.SignupRequest;
import com.xclone.auth.model.RefreshTokenData;
import com.xclone.config.AuthProperties;
import com.xclone.exception.custom.AccountNotActiveException;
import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.exception.custom.InvalidRefreshTokenException;
import com.xclone.security.jwt.JwtTokenProvider;
import com.xclone.user.model.entity.User;
import com.xclone.user.model.enums.UserStatus;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
  void signup_createsUserWithDisplayName_returnsAuthTokens() {
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

  @Test
  void logout_removesRefreshToken() {
    // Arrange
    String exampleUserId = UUID.randomUUID().toString();
    RefreshTokenData sampleToken = RefreshTokenData.create(exampleUserId, new AuthProperties());
    String exampleRefreshTokenId = UUID.randomUUID().toString();
    String exampleAccessToken = UUID.randomUUID().toString();

    when(this.refreshTokenService.getToken(anyString())).thenReturn(sampleToken);
    when(this.jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(exampleUserId);

    // Act
    this.authenticationService.logout(exampleAccessToken, exampleRefreshTokenId);

    // Assert
    verify(this.refreshTokenService, times(1)).removeToken(exampleRefreshTokenId);
  }

  @Test
  void logout_expiredRefreshToken_returnsInvalidRefreshToken() {
    // Arrange
    RefreshTokenData sampleToken = mock(RefreshTokenData.class);
    String exampleRefreshTokenId = UUID.randomUUID().toString();
    String exampleAccessToken = UUID.randomUUID().toString();

    when(this.refreshTokenService.getToken(anyString())).thenReturn(sampleToken);
    when(sampleToken.isExpired()).thenReturn(true);

    // Act
    assertThatThrownBy(
        () -> this.authenticationService.logout(exampleAccessToken, exampleRefreshTokenId))
        .isInstanceOf(InvalidRefreshTokenException.class)
        .hasMessage("Invalid refresh token");

    // Assert
    verify(this.refreshTokenService, times(0)).removeToken(exampleRefreshTokenId);
  }

  @Test
  void logout_tokenMismatch_returnsInvalidRefreshToken() {
    // Arrange
    String accessTokenUserId = UUID.randomUUID().toString();  // different IDs
    String refreshTokenUserId = UUID.randomUUID().toString(); // will naturally not match
    String exampleRefreshTokenId = UUID.randomUUID().toString();
    String exampleAccessToken = UUID.randomUUID().toString();


    RefreshTokenData sampleToken = mock(RefreshTokenData.class);
    when(sampleToken.isExpired()).thenReturn(false);
    when(sampleToken.userId()).thenReturn(refreshTokenUserId);

    when(this.refreshTokenService.getToken(anyString())).thenReturn(sampleToken);
    when(this.jwtTokenProvider.getUserIdFromToken(exampleAccessToken)).thenReturn(
        accessTokenUserId);

    // Act
    assertThatThrownBy(
        () -> this.authenticationService.logout(exampleAccessToken, exampleRefreshTokenId))
        .isInstanceOf(InvalidRefreshTokenException.class)
        .hasMessage("Invalid refresh token");

    // Assert
    verify(this.refreshTokenService, times(0)).removeToken(exampleRefreshTokenId);
  }

  @Test
  void refresh_rotatesToken_returnsNewAuthTokens() {
    // Arrange
    User exampleUser = new User();
    exampleUser.setDisplayName("exampleHandle");
    exampleUser.setId(UUID.randomUUID());
    String exampleUserId = exampleUser.getId().toString();

    RefreshTokenData sampleToken = RefreshTokenData.create(exampleUserId, new AuthProperties());
    String inputRefreshTokenId = UUID.randomUUID().toString();
    String newRefreshTokenId = UUID.randomUUID().toString();
    String newAccessToken = UUID.randomUUID().toString();

    when(this.refreshTokenService.getToken(anyString())).thenReturn(sampleToken);
    when(this.userRepository.findById(UUID.fromString(exampleUserId))).thenReturn(
        Optional.of(exampleUser));
    when(this.refreshTokenService.rotateToken(inputRefreshTokenId)).thenReturn(newRefreshTokenId);
    when(this.jwtTokenProvider.createToken(exampleUserId, "USER")).thenReturn(newAccessToken);

    // Act
    AuthTokens res = this.authenticationService.refresh(inputRefreshTokenId);

    // Assert
    assertThat(res.refreshToken()).isEqualTo(newRefreshTokenId);
    assertThat(res.accessToken()).isEqualTo(newAccessToken);
    assertThat(res.userId()).isEqualTo(exampleUserId);
  }

  @Test
  void refresh_expiredRefreshToken_returnsInvalidRefreshToken() {
    // Arrange
    RefreshTokenData sampleToken = mock(RefreshTokenData.class);
    String exampleRefreshTokenId = UUID.randomUUID().toString();

    when(this.refreshTokenService.getToken(anyString())).thenReturn(sampleToken);
    when(sampleToken.isExpired()).thenReturn(true);

    // Act
    assertThatThrownBy(
        () -> this.authenticationService.refresh(exampleRefreshTokenId))
        .isInstanceOf(InvalidRefreshTokenException.class)
        .hasMessage("Invalid refresh token");
  }


  @Test
  void refresh_userDoesNotExist_removesRefreshToken_returnsUsernameNotFound() {
    // Arrange
    RefreshTokenData sampleToken = mock(RefreshTokenData.class);
    String exampleRefreshTokenId = UUID.randomUUID().toString();
    String exampleUserId = UUID.randomUUID().toString();

    when(this.refreshTokenService.getToken(anyString())).thenReturn(sampleToken);
    when(sampleToken.isExpired()).thenReturn(false);
    when(sampleToken.userId()).thenReturn(exampleUserId);
    when(userRepository.findById(UUID.fromString(exampleUserId))).thenReturn(Optional.empty());

    // Act
    assertThatThrownBy(
        () -> this.authenticationService.refresh(exampleRefreshTokenId))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessage("User not found");

    // Assert
    verify(this.refreshTokenService, times(1)).removeToken(exampleRefreshTokenId);
  }

  @Test
  void refresh_userStatusNotActive_returnsAccountNotActive() {
    // Arrange
    User exampleUser = new User();
    exampleUser.setId(UUID.randomUUID());
    exampleUser.setStatus(UserStatus.SUSPENDED);

    RefreshTokenData sampleToken = mock(RefreshTokenData.class);
    String exampleRefreshTokenId = UUID.randomUUID().toString();

    when(this.refreshTokenService.getToken(anyString())).thenReturn(sampleToken);
    when(sampleToken.isExpired()).thenReturn(false);
    when(sampleToken.userId()).thenReturn(exampleUser.getId().toString());
    when(this.userRepository.findById(exampleUser.getId())).thenReturn(Optional.of(exampleUser));

    // Act
    assertThatThrownBy(
        () -> this.authenticationService.refresh(exampleRefreshTokenId))
        .isInstanceOf(AccountNotActiveException.class)
        .hasMessage("Account not active");

    // Assert
    verify(this.refreshTokenService, times(1)).removeToken(exampleRefreshTokenId);
  }
}
