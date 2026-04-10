package org.example.BenhAnDienTu.identity.application;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import org.example.BenhAnDienTu.identity.infrastructure.IdentityTokenStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdentityResetTokenService {

  private static final String PURPOSE = "RESET_PASSWORD";
  private static final String ISSUER = "EMR-backend";
  private static final int MIN_SIGNER_KEY_BYTES = 64;

  private final IdentityTokenStore tokenStore;
  private final Clock clock;

  @Value("${jwt.signer-key}")
  private String signerKey;

  @Value("${reset-token.valid-minutes:10}")
  private long validMinutes;

  public IdentityResetTokenService(IdentityTokenStore tokenStore, Clock clock) {
    this.tokenStore = tokenStore;
    this.clock = clock;
  }

  @PostConstruct
  void validateConfiguration() {
    if (!StringUtils.hasText(signerKey)) {
      throw new IllegalStateException(
          "Missing JWT signer key. Provide JWT_SIGNER_KEY via environment variable.");
    }

    int keyLength = signerKey.getBytes(StandardCharsets.UTF_8).length;
    if (keyLength < MIN_SIGNER_KEY_BYTES) {
      throw new IllegalStateException(
          "JWT signer key is too short. Use at least 64 bytes for HS512 signing.");
    }
  }

  public String issueToken(String actorId, String email) {
    Instant now = Instant.now(clock);
    Instant expiresAt = now.plus(validMinutes, ChronoUnit.MINUTES);
    String tokenId = UUID.randomUUID().toString();

    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .issuer(ISSUER)
            .subject(email)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiresAt))
            .jwtID(tokenId)
            .claim("purpose", PURPOSE)
            .claim("actor_id", actorId)
            .claim("email", email)
            .build();

    JWSObject jwsObject =
        new JWSObject(new JWSHeader(JWSAlgorithm.HS512), new Payload(claimsSet.toJSONObject()));
    try {
      jwsObject.sign(new MACSigner(signerKey.getBytes()));
      return jwsObject.serialize();
    } catch (JOSEException exception) {
      throw new IllegalStateException("Failed to issue reset token.", exception);
    }
  }

  public ResetTokenPayload verifyToken(String token) {
    if (token == null || token.isBlank()) {
      throw resetTokenException(HttpStatus.UNAUTHORIZED, "Reset token is required.");
    }

    SignedJWT signedJwt;
    try {
      signedJwt = SignedJWT.parse(token);
    } catch (ParseException exception) {
      throw resetTokenException(HttpStatus.UNAUTHORIZED, "Reset token is malformed.");
    }

    verifySignature(signedJwt);
    JWTClaimsSet claims = readClaims(signedJwt);

    Date expiration = claims.getExpirationTime();
    if (expiration == null || expiration.toInstant().isBefore(Instant.now(clock))) {
      throw resetTokenException(HttpStatus.UNAUTHORIZED, "Reset token has expired.");
    }

    String purpose = stringClaim(claims, "purpose");
    if (!PURPOSE.equals(purpose)) {
      throw resetTokenException(HttpStatus.UNAUTHORIZED, "Reset token purpose is invalid.");
    }

    String tokenId = claims.getJWTID();
    if (tokenId == null || tokenId.isBlank() || tokenStore.isInvalidated(tokenId)) {
      throw resetTokenException(HttpStatus.UNAUTHORIZED, "Reset token is invalid.");
    }

    String actorId = stringClaim(claims, "actor_id");
    String email = stringClaim(claims, "email");
    if (actorId == null || actorId.isBlank() || email == null || email.isBlank()) {
      throw resetTokenException(HttpStatus.UNAUTHORIZED, "Reset token payload is invalid.");
    }

    return new ResetTokenPayload(tokenId, actorId, email, expiration.toInstant());
  }

  public void invalidate(ResetTokenPayload payload) {
    tokenStore.invalidate(payload.tokenId(), payload.expiresAt());
  }

  public long validSeconds() {
    return validMinutes * 60;
  }

  private void verifySignature(SignedJWT signedJwt) {
    try {
      JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
      if (!signedJwt.verify(verifier)) {
        throw resetTokenException(HttpStatus.UNAUTHORIZED, "Reset token signature is invalid.");
      }
    } catch (JOSEException exception) {
      throw resetTokenException(
          HttpStatus.UNAUTHORIZED, "Reset token signature verification failed.");
    }
  }

  private static JWTClaimsSet readClaims(SignedJWT signedJwt) {
    try {
      return signedJwt.getJWTClaimsSet();
    } catch (ParseException exception) {
      throw resetTokenException(HttpStatus.UNAUTHORIZED, "Reset token claims are invalid.");
    }
  }

  private static String stringClaim(JWTClaimsSet claimsSet, String claimName) {
    Object claim = claimsSet.getClaim(claimName);
    return claim == null ? null : claim.toString();
  }

  private static ResponseStatusException resetTokenException(HttpStatus status, String message) {
    return new ResponseStatusException(status, message);
  }

  public record ResetTokenPayload(
      String tokenId, String actorId, String email, Instant expiresAt) {}
}
