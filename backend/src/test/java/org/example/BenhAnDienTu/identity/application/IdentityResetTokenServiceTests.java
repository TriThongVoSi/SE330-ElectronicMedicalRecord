package org.example.BenhAnDienTu.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
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
class IdentityResetTokenServiceTests {

  private static final String SIGNER_KEY =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789__";

  @Mock private IdentityTokenStore tokenStore;

  private IdentityResetTokenService service;
  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2026-04-03T10:15:30Z"), ZoneOffset.UTC);
    service = new IdentityResetTokenService(tokenStore, fixedClock);
    ReflectionTestUtils.setField(service, "signerKey", SIGNER_KEY);
    ReflectionTestUtils.setField(service, "validMinutes", 10L);
  }

  @Test
  void issueAndVerifyShouldReturnPayload() {
    String token = service.issueToken("doctor-1", "doctor.local@EMR.dev");

    IdentityResetTokenService.ResetTokenPayload payload = service.verifyToken(token);

    assertThat(payload.actorId()).isEqualTo("doctor-1");
    assertThat(payload.email()).isEqualTo("doctor.local@EMR.dev");
    assertThat(payload.tokenId()).isNotBlank();
    assertThat(payload.expiresAt()).isAfter(Instant.now(fixedClock));
  }

  @Test
  void verifyTokenShouldRejectRevokedToken() {
    String token = service.issueToken("doctor-1", "doctor.local@EMR.dev");
    when(tokenStore.isInvalidated(anyString())).thenReturn(true);

    assertThatThrownBy(() -> service.verifyToken(token))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
              assertThat(exception.getReason()).isEqualTo("Reset token is invalid.");
            });
  }

  @Test
  void verifyTokenShouldRejectInvalidPurpose() {
    String token =
        buildToken(
            UUID.randomUUID().toString(),
            "doctor-1",
            "doctor.local@EMR.dev",
            "NOT_RESET",
            Instant.now(fixedClock),
            Instant.now(fixedClock).plus(Duration.ofMinutes(10)));

    assertThatThrownBy(() -> service.verifyToken(token))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
              assertThat(exception.getReason()).isEqualTo("Reset token purpose is invalid.");
            });
  }

  @Test
  void verifyTokenShouldRejectExpiredToken() {
    String token = service.issueToken("doctor-1", "doctor.local@EMR.dev");

    IdentityResetTokenService expiredVerifier =
        new IdentityResetTokenService(tokenStore, Clock.offset(fixedClock, Duration.ofMinutes(20)));
    ReflectionTestUtils.setField(expiredVerifier, "signerKey", SIGNER_KEY);
    ReflectionTestUtils.setField(expiredVerifier, "validMinutes", 10L);

    assertThatThrownBy(() -> expiredVerifier.verifyToken(token))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
              assertThat(exception.getReason()).isEqualTo("Reset token has expired.");
            });
  }

  @Test
  void invalidateShouldPersistTokenIdentifier() {
    String token = service.issueToken("doctor-1", "doctor.local@EMR.dev");
    IdentityResetTokenService.ResetTokenPayload payload = service.verifyToken(token);

    service.invalidate(payload);

    verify(tokenStore).invalidate(payload.tokenId(), payload.expiresAt());
  }

  private static String buildToken(
      String tokenId,
      String actorId,
      String email,
      String purpose,
      Instant issuedAt,
      Instant expiresAt) {
    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .issuer("EMR-backend")
            .subject(email)
            .issueTime(Date.from(issuedAt))
            .expirationTime(Date.from(expiresAt))
            .jwtID(tokenId)
            .claim("purpose", purpose)
            .claim("actor_id", actorId)
            .claim("email", email)
            .build();

    JWSObject jwsObject =
        new JWSObject(new JWSHeader(JWSAlgorithm.HS512), new Payload(claimsSet.toJSONObject()));
    try {
      jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
      return jwsObject.serialize();
    } catch (JOSEException exception) {
      throw new IllegalStateException("Failed to create token for test.", exception);
    }
  }
}
