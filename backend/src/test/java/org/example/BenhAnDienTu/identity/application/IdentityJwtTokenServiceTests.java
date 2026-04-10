package org.example.BenhAnDienTu.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.example.BenhAnDienTu.identity.api.AuthUserView;
import org.example.BenhAnDienTu.identity.infrastructure.IdentityTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class IdentityJwtTokenServiceTests {

  private static final String SIGNER_KEY =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789__";

  @Mock private IdentityTokenStore tokenStore;

  private IdentityJwtTokenService service;
  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2026-04-03T10:15:30Z"), ZoneOffset.UTC);
    service = new IdentityJwtTokenService(tokenStore, fixedClock);
    ReflectionTestUtils.setField(service, "signerKey", SIGNER_KEY);
    ReflectionTestUtils.setField(service, "accessTokenValidDurationSeconds", 300L);
    ReflectionTestUtils.setField(service, "refreshTokenValidDurationSeconds", 3600L);
  }

  @Test
  void issueAndVerifyShouldReturnClaimsForAccessAndRefreshTokens() {
    AuthUserView user =
        new AuthUserView(
            "admin-1", "admin.local", "Admin Local", "admin.local@EMR.dev", "ADMIN", false);

    IdentityJwtTokenService.TokenPair tokenPair = service.issueTokenPair(user);
    IdentityJwtTokenService.TokenClaims accessClaims =
        service.verifyAccessToken(tokenPair.accessToken());
    IdentityJwtTokenService.TokenClaims refreshClaims =
        service.verifyRefreshToken(tokenPair.refreshToken());

    assertThat(accessClaims.actorId()).isEqualTo("admin-1");
    assertThat(accessClaims.username()).isEqualTo("admin.local");
    assertThat(accessClaims.email()).isEqualTo("admin.local@EMR.dev");
    assertThat(accessClaims.role()).isEqualTo("ADMIN");
    assertThat(accessClaims.tokenType()).isEqualTo("ACCESS");

    assertThat(refreshClaims.actorId()).isEqualTo("admin-1");
    assertThat(refreshClaims.tokenType()).isEqualTo("REFRESH");
  }

  @Test
  void verifyRefreshTokenShouldRejectAccessToken() {
    AuthUserView user =
        new AuthUserView(
            "doctor-1", "doctor.local", "Doctor Local", "doctor.local@EMR.dev", "DOCTOR", false);
    IdentityJwtTokenService.TokenPair tokenPair = service.issueTokenPair(user);

    assertThatThrownBy(() -> service.verifyRefreshToken(tokenPair.accessToken()))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
              assertThat(exception.getReason()).isEqualTo("Token type is invalid.");
            });
  }

  @Test
  void verifyAccessTokenShouldRejectRevokedToken() {
    AuthUserView user =
        new AuthUserView(
            "doctor-1", "doctor.local", "Doctor Local", "doctor.local@EMR.dev", "DOCTOR", false);
    IdentityJwtTokenService.TokenPair tokenPair = service.issueTokenPair(user);
    when(tokenStore.isInvalidated(anyString())).thenReturn(true);

    assertThatThrownBy(() -> service.verifyAccessToken(tokenPair.accessToken()))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
              assertThat(exception.getReason()).isEqualTo("Token has been revoked.");
            });
  }

  @Test
  void verifyAccessTokenShouldRejectExpiredToken() {
    AuthUserView user =
        new AuthUserView(
            "doctor-1", "doctor.local", "Doctor Local", "doctor.local@EMR.dev", "DOCTOR", false);
    IdentityJwtTokenService.TokenPair tokenPair = service.issueTokenPair(user);

    IdentityJwtTokenService expiredVerifier =
        new IdentityJwtTokenService(tokenStore, Clock.offset(fixedClock, Duration.ofHours(1)));
    ReflectionTestUtils.setField(expiredVerifier, "signerKey", SIGNER_KEY);
    ReflectionTestUtils.setField(expiredVerifier, "accessTokenValidDurationSeconds", 300L);
    ReflectionTestUtils.setField(expiredVerifier, "refreshTokenValidDurationSeconds", 3600L);

    assertThatThrownBy(() -> expiredVerifier.verifyAccessToken(tokenPair.accessToken()))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
              assertThat(exception.getReason()).isEqualTo("Token has expired.");
            });
  }

  @Test
  void invalidateTokenShouldPersistJti() {
    AuthUserView user =
        new AuthUserView(
            "doctor-1", "doctor.local", "Doctor Local", "doctor.local@EMR.dev", "DOCTOR", false);
    IdentityJwtTokenService.TokenPair tokenPair = service.issueTokenPair(user);
    IdentityJwtTokenService.TokenClaims claims =
        service.verifyRefreshToken(tokenPair.refreshToken());

    service.invalidateToken(claims);

    verify(tokenStore).invalidate(claims.jwtId(), claims.expiresAt());
  }
}
