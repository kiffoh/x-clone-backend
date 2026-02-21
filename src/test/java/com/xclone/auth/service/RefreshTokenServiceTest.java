package com.xclone.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xclone.auth.model.RefreshTokenData;
import com.xclone.auth.repository.RefreshTokenRepository;
import com.xclone.config.AuthProperties;
import com.xclone.exception.custom.InvalidRefreshTokenException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {
  @Mock
  RefreshTokenRepository refreshTokenRepository;
  AuthProperties authProperties;

  RefreshTokenService refreshTokenService;

  private ArgumentCaptor<String> tokenIdCaptor;

  @BeforeEach
  void setUp() {
    authProperties = new AuthProperties();

    refreshTokenService =
        new RefreshTokenService(refreshTokenRepository, authProperties);

  }

  @Test
  public void createToken_persistsCorrectMetadata_andReturnsTokenId() {
    tokenIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<RefreshTokenData> metadataCaptor =
        ArgumentCaptor.forClass(RefreshTokenData.class);

    String userId = UUID.randomUUID().toString();

    String returnedTokenId = this.refreshTokenService.createToken(userId);
    verify(this.refreshTokenRepository, times(1))
        .save(tokenIdCaptor.capture(), metadataCaptor.capture());

    String capturedTokenId = tokenIdCaptor.getValue();
    RefreshTokenData capturedMetadata = metadataCaptor.getValue();

    assertThat(capturedTokenId).isEqualTo(returnedTokenId);
    assertThat(capturedMetadata.userId()).isEqualTo(userId);

    assertThat(capturedMetadata.expiresAt())
        .isAfter(Instant.now());
  }

  @Test
  public void getToken_returnsTokenId() {
    tokenIdCaptor = ArgumentCaptor.forClass(String.class);

    String tokenId = UUID.randomUUID().toString();

    this.refreshTokenService.getToken(tokenId);
    verify(this.refreshTokenRepository, times(1)).find(
        tokenIdCaptor.capture()
    );

    // Is there a convention for captured.is equal to input or the other way around?
    String capturedTokenId = tokenIdCaptor.getValue();
    assertThat(capturedTokenId).isEqualTo(tokenId);
  }

  @Test
  public void removeToken_deletesToken() {
    tokenIdCaptor = ArgumentCaptor.forClass(String.class);

    String tokenId = UUID.randomUUID().toString();

    this.refreshTokenService.removeToken(tokenId);
    verify(this.refreshTokenRepository, times(1)).delete(
        tokenIdCaptor.capture()
    );

    // Is there a convention for captured.is equal to input or the other way around?
    String capturedTokenId = tokenIdCaptor.getValue();
    assertThat(capturedTokenId).isEqualTo(tokenId);
  }

  @Test
  public void rotateToken_validatesToken_removesToken_andReturnsTokenId() {
    // Captor initialisation
    ArgumentCaptor<String> findTokenIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> saveTokenIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> deleteTokenIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<RefreshTokenData> saveMetadataCaptor =
        ArgumentCaptor.forClass(RefreshTokenData.class);
    // Refresh token repository mock return value
    // Should this be in a fixtures file?
    RefreshTokenData validRefreshToken =
        RefreshTokenData.create(UUID.randomUUID().toString(), authProperties);
    when(this.refreshTokenRepository.find(anyString())).thenReturn(validRefreshToken);

    // Act
    String tokenId = UUID.randomUUID().toString();
    String newTokenId = this.refreshTokenService.rotateToken(tokenId);

    // Capture repository calls
    verify(this.refreshTokenRepository, times(1)).find(
        findTokenIdCaptor.capture()
    );
    verify(this.refreshTokenRepository, times(1)).save(
        saveTokenIdCaptor.capture(),
        saveMetadataCaptor.capture()
    );
    verify(this.refreshTokenRepository, times(1)).delete(
        deleteTokenIdCaptor.capture()
    );

    // Assertions

    // Assert that tokenId provided is used for .find()
    String findTokenId = findTokenIdCaptor.getValue();
    assertThat(findTokenId).isEqualTo(tokenId);

    // Assert newTokenId used for .save() is returned
    String saveTokenId = saveTokenIdCaptor.getValue();
    assertThat(saveTokenId).isEqualTo(newTokenId);

    // Assert that the old token is deleted
    String deleteTokenId = deleteTokenIdCaptor.getValue();
    assertThat(deleteTokenId).isEqualTo(tokenId);
  }

  @Test
  public void rotateToken_returnsInvalidRefreshTokenException() {
    // Refresh token repository mock return value
    // Should this be in a fixtures file?
    RefreshTokenData invalidRefreshToken = new RefreshTokenData(UUID.randomUUID().toString(),
        Instant.now().minusSeconds(120), Instant.now().minusSeconds(60));
    when(this.refreshTokenRepository.find(anyString())).thenReturn(invalidRefreshToken);

    // Act & Assert: assert the exception is thrown
    String tokenId = UUID.randomUUID().toString();
    assertThatThrownBy(() -> this.refreshTokenService.rotateToken(tokenId))
        .isInstanceOf(InvalidRefreshTokenException.class)
        .hasMessageContaining("Refresh token is invalid");
  }
}
