package org.example.BenhAnDienTu.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.example.BenhAnDienTu.identity.domain.OtpPurpose;
import org.example.BenhAnDienTu.identity.infrastructure.IdentityOtpStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class IdentityOtpServiceTests {

  @Mock private IdentityOtpStore otpStore;

  private IdentityOtpService service;
  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2026-04-03T10:15:30Z"), ZoneOffset.UTC);
    service = new IdentityOtpService(otpStore, fixedClock);
    ReflectionTestUtils.setField(service, "expiryMinutes", 5L);
    ReflectionTestUtils.setField(service, "maxAttempts", 5);
    ReflectionTestUtils.setField(service, "resendCooldownSeconds", 60L);
    ReflectionTestUtils.setField(service, "otpHashSecret", "otp-secret");
  }

  @Test
  void sendOtpShouldSaveNewChallengeWhenNoExistingChallenge() {
    when(otpStore.findLatestActive("doctor@EMR.dev", OtpPurpose.REGISTER))
        .thenReturn(Optional.empty());

    IdentityOtpService.OtpChallenge challenge =
        service.sendOtp("doctor@EMR.dev", "doctor-1", OtpPurpose.REGISTER, true);

    ArgumentCaptor<IdentityOtpStore.NewOtpChallenge> captor =
        ArgumentCaptor.forClass(IdentityOtpStore.NewOtpChallenge.class);
    verify(otpStore).saveNewChallenge(captor.capture());
    IdentityOtpStore.NewOtpChallenge saved = captor.getValue();

    assertThat(saved.userId()).isEqualTo("doctor-1");
    assertThat(saved.email()).isEqualTo("doctor@EMR.dev");
    assertThat(saved.purpose()).isEqualTo(OtpPurpose.REGISTER);
    assertThat(saved.otpHash()).isNotBlank();
    assertThat(saved.attempts()).isZero();
    assertThat(saved.maxAttempts()).isEqualTo(5);
    assertThat(saved.resendCount()).isEqualTo(1);
    assertThat(challenge.emailMasked()).isEqualTo("d***r@EMR.dev");
    assertThat(challenge.expiresInSeconds()).isEqualTo(300);
  }

  @Test
  void sendOtpShouldConsumeOldChallengeAndIncreaseResendCount() {
    LocalDateTime now = LocalDateTime.now(fixedClock);
    IdentityOtpStore.OtpVerificationRow active =
        otpRow(
            "otp-old",
            OtpPurpose.REGISTER,
            hashOtp("123456"),
            now.plusMinutes(2),
            0,
            5,
            now.minusSeconds(120),
            2);
    when(otpStore.findLatestActive("doctor@EMR.dev", OtpPurpose.REGISTER))
        .thenReturn(Optional.of(active));

    service.sendOtp("doctor@EMR.dev", "doctor-1", OtpPurpose.REGISTER, true);

    verify(otpStore).markConsumed("otp-old", now);
    ArgumentCaptor<IdentityOtpStore.NewOtpChallenge> captor =
        ArgumentCaptor.forClass(IdentityOtpStore.NewOtpChallenge.class);
    verify(otpStore).saveNewChallenge(captor.capture());
    assertThat(captor.getValue().resendCount()).isEqualTo(3);
  }

  @Test
  void sendOtpShouldRejectWhenCooldownWindowHasNotElapsed() {
    LocalDateTime now = LocalDateTime.now(fixedClock);
    IdentityOtpStore.OtpVerificationRow active =
        otpRow(
            "otp-old",
            OtpPurpose.REGISTER,
            hashOtp("123456"),
            now.plusMinutes(2),
            0,
            5,
            now.minusSeconds(30),
            1);
    when(otpStore.findLatestActive("doctor@EMR.dev", OtpPurpose.REGISTER))
        .thenReturn(Optional.of(active));

    assertThatThrownBy(
            () -> service.sendOtp("doctor@EMR.dev", "doctor-1", OtpPurpose.REGISTER, true))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
              assertThat(exception.getReason())
                  .isEqualTo("OTP was sent recently. Please wait before requesting again.");
            });

    verify(otpStore, never()).markConsumed(any(), any());
    verify(otpStore, never()).saveNewChallenge(any());
  }

  @Test
  void verifyOtpShouldConsumeChallengeWhenCodeMatches() {
    LocalDateTime now = LocalDateTime.now(fixedClock);
    IdentityOtpStore.OtpVerificationRow challenge =
        otpRow(
            "otp-1",
            OtpPurpose.RESET_PASSWORD,
            hashOtp("123456"),
            now.plusMinutes(2),
            0,
            5,
            now.minusSeconds(70),
            1);
    when(otpStore.findLatestActive("doctor@EMR.dev", OtpPurpose.RESET_PASSWORD))
        .thenReturn(Optional.of(challenge));

    service.verifyOtp("doctor@EMR.dev", OtpPurpose.RESET_PASSWORD, "123456");

    verify(otpStore).markConsumed("otp-1", now);
    verify(otpStore, never()).updateAttempts(any(), anyInt());
  }

  @Test
  void verifyOtpShouldIncreaseAttemptsWhenCodeIsInvalid() {
    LocalDateTime now = LocalDateTime.now(fixedClock);
    IdentityOtpStore.OtpVerificationRow challenge =
        otpRow(
            "otp-1",
            OtpPurpose.RESET_PASSWORD,
            hashOtp("123456"),
            now.plusMinutes(2),
            0,
            5,
            now.minusSeconds(70),
            1);
    when(otpStore.findLatestActive("doctor@EMR.dev", OtpPurpose.RESET_PASSWORD))
        .thenReturn(Optional.of(challenge));

    assertThatThrownBy(
            () -> service.verifyOtp("doctor@EMR.dev", OtpPurpose.RESET_PASSWORD, "999999"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(exception.getReason()).isEqualTo("OTP is invalid.");
            });

    verify(otpStore).updateAttempts("otp-1", 1);
    verify(otpStore, never()).markConsumed(any(), any());
  }

  @Test
  void verifyOtpShouldConsumeChallengeWhenMaximumAttemptsReached() {
    LocalDateTime now = LocalDateTime.now(fixedClock);
    IdentityOtpStore.OtpVerificationRow challenge =
        otpRow(
            "otp-1",
            OtpPurpose.RESET_PASSWORD,
            hashOtp("123456"),
            now.plusMinutes(2),
            4,
            5,
            now.minusSeconds(70),
            1);
    when(otpStore.findLatestActive("doctor@EMR.dev", OtpPurpose.RESET_PASSWORD))
        .thenReturn(Optional.of(challenge));

    assertThatThrownBy(
            () -> service.verifyOtp("doctor@EMR.dev", OtpPurpose.RESET_PASSWORD, "999999"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
              assertThat(exception.getReason())
                  .isEqualTo("Too many OTP attempts. Please request a new code.");
            });

    verify(otpStore).updateAttempts("otp-1", 5);
    verify(otpStore).markConsumed("otp-1", now);
  }

  @Test
  void verifyOtpShouldRejectWhenNoActiveChallengeFound() {
    when(otpStore.findLatestActive("doctor@EMR.dev", OtpPurpose.RESET_PASSWORD))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.verifyOtp("doctor@EMR.dev", OtpPurpose.RESET_PASSWORD, "123456"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(exception.getReason()).isEqualTo("OTP is invalid or unavailable.");
            });
  }

  @Test
  void validateConfigurationShouldRejectMissingHashSecret() {
    ReflectionTestUtils.setField(service, "otpHashSecret", " ");

    assertThatThrownBy(service::validateConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Missing OTP hash secret. Provide OTP_HASH_SECRET via environment variable.");
  }

  @Test
  void validateConfigurationShouldRejectWeakHashSecret() {
    ReflectionTestUtils.setField(service, "otpHashSecret", "too-short-secret");

    assertThatThrownBy(service::validateConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "OTP hash secret is too short. Use at least 32 characters for OTP_HASH_SECRET.");
  }

  private String hashOtp(String otp) {
    return (String) ReflectionTestUtils.invokeMethod(service, "hashOtp", otp);
  }

  private static IdentityOtpStore.OtpVerificationRow otpRow(
      String otpId,
      OtpPurpose purpose,
      String otpHash,
      LocalDateTime expiresAt,
      int attempts,
      int maxAttempts,
      LocalDateTime lastSentAt,
      int resendCount) {
    return new IdentityOtpStore.OtpVerificationRow(
        otpId,
        "user-1",
        "doctor@EMR.dev",
        purpose,
        otpHash,
        LocalDateTime.parse("2026-04-03T10:10:30"),
        expiresAt,
        attempts,
        maxAttempts,
        null,
        lastSentAt,
        resendCount);
  }
}
