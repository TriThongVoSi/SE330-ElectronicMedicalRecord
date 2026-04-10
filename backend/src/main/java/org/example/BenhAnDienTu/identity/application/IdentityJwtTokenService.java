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
import org.example.BenhAnDienTu.identity.api.AuthUserView;
import org.example.BenhAnDienTu.identity.infrastructure.IdentityTokenStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdentityJwtTokenService {

  private static final String TOKEN_TYPE_ACCESS = "ACCESS";
  private static final String TOKEN_TYPE_REFRESH = "REFRESH";
  private static final String ISSUER = "EMR-backend";
  private static final int MIN_SIGNER_KEY_BYTES = 64;

  private final IdentityTokenStore tokenStore;
  private final Clock clock;

  @Value("${jwt.signer-key}")
  private String signerKey;

  @Value("${jwt.valid-duration}")
  private long accessTokenValidDurationSeconds;

  @Value("${jwt.refreshable-duration}")
  private long refreshTokenValidDurationSeconds;

  public IdentityJwtTokenService(IdentityTokenStore tokenStore, Clock clock) {
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

  public TokenPair issueTokenPair(AuthUserView user) {
    Instant now = Instant.now(clock);
    String accessToken = issueToken(user, now, accessTokenValidDurationSeconds, TOKEN_TYPE_ACCESS);
    String refreshToken =
        issueToken(user, now, refreshTokenValidDurationSeconds, TOKEN_TYPE_REFRESH);
    return new TokenPair(accessToken, refreshToken);
  }

  public TokenClaims verifyAccessToken(String token) {
    return verifyToken(token, TOKEN_TYPE_ACCESS);
  }

  public TokenClaims verifyRefreshToken(String token) {
    return verifyToken(token, TOKEN_TYPE_REFRESH);
  }

  public void invalidateToken(TokenClaims claims) {
    tokenStore.invalidate(claims.jwtId(), claims.expiresAt());
  }

  public long accessTokenValidDurationSeconds() {
    return accessTokenValidDurationSeconds;
  }

  private String issueToken(
      AuthUserView user, Instant now, long durationSeconds, String expectedTokenType) {
    Instant expiresAt = now.plus(durationSeconds, ChronoUnit.SECONDS);
    String tokenId = UUID.randomUUID().toString();

    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .subject(user.username())
            .issuer(ISSUER)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiresAt))
            .jwtID(tokenId)
            .claim("token_type", expectedTokenType)
            .claim("actor_id", user.id())
            .claim("username", user.username())
            .claim("email", user.email())
            .claim("role", user.role())
            .claim("scope", "ROLE_" + user.role())
            .build();

    JWSObject jwsObject =
        new JWSObject(new JWSHeader(JWSAlgorithm.HS512), new Payload(claimsSet.toJSONObject()));
    try {
      jwsObject.sign(new MACSigner(signerKey.getBytes()));
    } catch (JOSEException exception) {
      throw new IllegalStateException("Failed to sign JWT token.", exception);
    }

    return jwsObject.serialize();
  }

  private TokenClaims verifyToken(String token, String expectedTokenType) {
    SignedJWT signedJwt = parseToken(token);
    verifySignature(signedJwt);

    JWTClaimsSet claimsSet = readClaims(signedJwt);
    Date expiration = claimsSet.getExpirationTime();
    Instant now = Instant.now(clock);
    if (expiration == null || expiration.toInstant().isBefore(now)) {
      throw authException("Token has expired.");
    }

    String tokenType = stringClaim(claimsSet, "token_type");
    if (!expectedTokenType.equals(tokenType)) {
      throw authException("Token type is invalid.");
    }

    String jwtId = claimsSet.getJWTID();
    if (jwtId == null || jwtId.isBlank()) {
      throw authException("Token identifier is missing.");
    }
    if (tokenStore.isInvalidated(jwtId)) {
      throw authException("Token has been revoked.");
    }

    String actorId = stringClaim(claimsSet, "actor_id");
    String username = stringClaim(claimsSet, "username");
    String email = stringClaim(claimsSet, "email");
    String role = stringClaim(claimsSet, "role");

    if (actorId == null || username == null || email == null || role == null) {
      throw authException("Token payload is incomplete.");
    }

    return new TokenClaims(
        actorId, username, email, role, tokenType, jwtId, expiration.toInstant(), token);
  }

  private SignedJWT parseToken(String token) {
    if (token == null || token.isBlank()) {
      throw authException("Token is required.");
    }
    try {
      return SignedJWT.parse(token);
    } catch (ParseException exception) {
      throw authException("Token format is invalid.");
    }
  }

  private void verifySignature(SignedJWT signedJwt) {
    try {
      JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
      boolean verified = signedJwt.verify(verifier);
      if (!verified) {
        throw authException("Token signature is invalid.");
      }
    } catch (JOSEException exception) {
      throw authException("Failed to verify token signature.");
    }
  }

  private JWTClaimsSet readClaims(SignedJWT signedJwt) {
    try {
      return signedJwt.getJWTClaimsSet();
    } catch (ParseException exception) {
      throw authException("Token payload cannot be parsed.");
    }
  }

  private static String stringClaim(JWTClaimsSet claimsSet, String claimName) {
    Object claimValue = claimsSet.getClaim(claimName);
    return claimValue == null ? null : claimValue.toString();
  }

  private static ResponseStatusException authException(String message) {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
  }

  public record TokenPair(String accessToken, String refreshToken) {}

  public record TokenClaims(
      String actorId,
      String username,
      String email,
      String role,
      String tokenType,
      String jwtId,
      Instant expiresAt,
      String rawToken) {}
}
