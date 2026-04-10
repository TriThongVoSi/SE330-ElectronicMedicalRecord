package org.example.BenhAnDienTu.identity.application;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.example.BenhAnDienTu.identity.domain.OtpPurpose;
import org.example.BenhAnDienTu.identity.infrastructure.IdentityOtpStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdentityOtpService {

  private static final Logger log = LoggerFactory.getLogger(IdentityOtpService.class);
  private static final SecureRandom OTP_RANDOM = new SecureRandom();
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final int MIN_OTP_HASH_SECRET_LENGTH = 32;

  private final IdentityOtpStore otpStore;
  private final Clock clock;

  @Value("${otp.expiry-minutes:5}")
  private long expiryMinutes;

  @Value("${otp.max-attempts:5}")
  private int maxAttempts;

  @Value("${otp.resend-cooldown-seconds:60}")
  private long resendCooldownSeconds;

  @Value("${otp.hash-secret}")
  private String otpHashSecret;

  public IdentityOtpService(IdentityOtpStore otpStore, Clock clock) {
    this.otpStore = otpStore;
    this.clock = clock;
  }

  @PostConstruct
  void validateConfiguration() {
    if (!StringUtils.hasText(otpHashSecret)) {
      throw new IllegalStateException(
          "Missing OTP hash secret. Provide OTP_HASH_SECRET via environment variable.");
    }

    if (otpHashSecret.trim().length() < MIN_OTP_HASH_SECRET_LENGTH) {
      throw new IllegalStateException(
          "OTP hash secret is too short. Use at least 32 characters for OTP_HASH_SECRET.");
    }
  }

  public OtpChallenge sendOtp(
      String email, String userId, OtpPurpose purpose, boolean enforceCooldown) {
    LocalDateTime now = LocalDateTime.now(clock);
    Optional<IdentityOtpStore.OtpVerificationRow> previousChallenge =
        consumePreviousChallenge(email, purpose, now, enforceCooldown);

    String plainOtp = generateOtpCode();
    String otpHash = hashOtp(plainOtp);
    LocalDateTime expiresAt = now.plusMinutes(expiryMinutes);
    int resendCount = previousChallenge.map(challenge -> challenge.resendCount() + 1).orElse(1);

    String otpId = UUID.randomUUID().toString();
    otpStore.saveNewChallenge(
        new IdentityOtpStore.NewOtpChallenge(
            otpId,
            userId,
            email,
            purpose,
            otpHash,
            now,
            expiresAt,
            0,
            maxAttempts,
            now,
            resendCount,
            null));

    String emailMasked = maskEmail(email);
    log.info(
        "OTP challenge issued: otpId={}, emailMasked={}, purpose={}, expiresAt={}",
        otpId,
        emailMasked,
        purpose,
        expiresAt);

    return new OtpChallenge(emailMasked, ChronoUnit.SECONDS.between(now, expiresAt));
  }

  public void verifyOtp(String email, OtpPurpose purpose, String otp) {
    LocalDateTime now = LocalDateTime.now(clock);
    IdentityOtpStore.OtpVerificationRow challenge =
        otpStore
            .findLatestActive(email, purpose)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "OTP is invalid or unavailable."));

    assertChallengeNotExpired(challenge, now);
    assertAttemptLimitNotExceeded(challenge, now);

    if (isOtpInvalid(challenge, otp)) {
      registerFailedAttempt(challenge, now);
    }

    otpStore.markConsumed(challenge.otpId(), now);
  }

  public long expirySeconds() {
    return expiryMinutes * 60;
  }

  private Optional<IdentityOtpStore.OtpVerificationRow> consumePreviousChallenge(
      String email, OtpPurpose purpose, LocalDateTime now, boolean enforceCooldown) {
    Optional<IdentityOtpStore.OtpVerificationRow> existingChallenge =
        otpStore.findLatestActive(email, purpose);

    existingChallenge.ifPresent(
        challenge -> {
          ensureCooldownWindowHasElapsed(challenge, now, enforceCooldown);
          otpStore.markConsumed(challenge.otpId(), now);
        });

    return existingChallenge;
  }

  private void ensureCooldownWindowHasElapsed(
      IdentityOtpStore.OtpVerificationRow challenge, LocalDateTime now, boolean enforceCooldown) {
    if (!enforceCooldown || challenge.lastSentAt() == null) {
      return;
    }

    LocalDateTime nextAllowedAt = challenge.lastSentAt().plusSeconds(resendCooldownSeconds);
    if (nextAllowedAt.isAfter(now)) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS,
          "OTP was sent recently. Please wait before requesting again.");
    }
  }

  private void assertChallengeNotExpired(
      IdentityOtpStore.OtpVerificationRow challenge, LocalDateTime now) {
    if (challenge.expiresAt().isBefore(now)) {
      otpStore.markConsumed(challenge.otpId(), now);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has expired.");
    }
  }

  private void assertAttemptLimitNotExceeded(
      IdentityOtpStore.OtpVerificationRow challenge, LocalDateTime now) {
    if (challenge.attempts() < challenge.maxAttempts()) {
      return;
    }

    otpStore.markConsumed(challenge.otpId(), now);
    throw new ResponseStatusException(
        HttpStatus.TOO_MANY_REQUESTS, "Too many OTP attempts. Please request a new code.");
  }

  private boolean isOtpInvalid(IdentityOtpStore.OtpVerificationRow challenge, String otp) {
    // Hash comparison ensures raw OTP values never leave this service or reach persistence.
    return !hashOtp(otp).equals(challenge.otpHash());
  }

  private void registerFailedAttempt(
      IdentityOtpStore.OtpVerificationRow challenge, LocalDateTime now) {
    int nextAttempts = challenge.attempts() + 1;
    otpStore.updateAttempts(challenge.otpId(), nextAttempts);

    if (nextAttempts >= challenge.maxAttempts()) {
      otpStore.markConsumed(challenge.otpId(), now);
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Too many OTP attempts. Please request a new code.");
    }

    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP is invalid.");
  }

  private String hashOtp(String otp) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(otpHashSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      return HexFormat.of().formatHex(mac.doFinal(otp.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to hash OTP.", exception);
    }
  }

  private static String generateOtpCode() {
    int numericOtp = OTP_RANDOM.nextInt(900_000) + 100_000;
    return Integer.toString(numericOtp);
  }

  private static String maskEmail(String email) {
    if (email == null || !email.contains("@")) {
      return "";
    }
    String[] parts = email.split("@", 2);
    String local = parts[0];
    String domain = parts[1];
    if (local.length() <= 2) {
      return "*@" + domain;
    }
    return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
  }

  public record OtpChallenge(String emailMasked, long expiresInSeconds) {}
}
