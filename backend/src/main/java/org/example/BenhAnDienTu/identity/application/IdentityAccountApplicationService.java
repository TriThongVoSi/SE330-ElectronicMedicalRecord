package org.example.BenhAnDienTu.identity.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.example.BenhAnDienTu.identity.api.AuthUserView;
import org.example.BenhAnDienTu.identity.domain.IdentityAccount;
import org.example.BenhAnDienTu.identity.domain.OtpPurpose;
import org.example.BenhAnDienTu.identity.infrastructure.IdentityDirectoryAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdentityAccountApplicationService {

  private static final String STATUS_ACTIVE = "ACTIVE";
  private static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";
  private static final String STATUS_LOCKED = "LOCKED";
  private static final String STATUS_INACTIVE = "INACTIVE";

  private final IdentityDirectoryAdapter directoryAdapter;
  private final IdentityOtpService otpService;
  private final IdentityResetTokenService resetTokenService;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;
  private final Map<String, PasswordResetRateWindow> passwordResetRateLimits =
      new ConcurrentHashMap<>();

  @Value("${password-reset.rate-limit.max:3}")
  private int passwordResetRateLimitMax;

  @Value("${password-reset.rate-limit.window-minutes:15}")
  private long passwordResetRateLimitWindowMinutes;

  public IdentityAccountApplicationService(
      IdentityDirectoryAdapter directoryAdapter,
      IdentityOtpService otpService,
      IdentityResetTokenService resetTokenService,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    this.directoryAdapter = directoryAdapter;
    this.otpService = otpService;
    this.resetTokenService = resetTokenService;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  public IdentityOtpService.OtpChallenge signUp(SignUpCommand command) {
    IdentityAccount existingByEmail = directoryAdapter.findByEmail(command.email()).orElse(null);
    if (existingByEmail != null) {
      if (isPending(existingByEmail)
          && existingByEmail.principal().equalsIgnoreCase(command.username())) {
        return otpService.sendOtp(
            existingByEmail.email(), existingByEmail.actorId(), OtpPurpose.REGISTER, true);
      }
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists.");
    }

    IdentityAccount existingByUsername =
        directoryAdapter.findByPrincipal(command.username()).orElse(null);
    if (existingByUsername != null) {
      if (isPending(existingByUsername)
          && existingByUsername.email().equalsIgnoreCase(command.email())) {
        return otpService.sendOtp(
            existingByUsername.email(), existingByUsername.actorId(), OtpPurpose.REGISTER, true);
      }
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists.");
    }

    try {
      IdentityAccount created =
          directoryAdapter.createUser(
              IdentityDirectoryAdapter.CreateUserCommand.pendingVerification(
                  command.username(),
                  command.email(),
                  passwordEncoder.encode(command.password()),
                  command.fullName(),
                  command.roleCode()));
      return otpService.sendOtp(created.email(), created.actorId(), OtpPurpose.REGISTER, true);
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists.");
    }
  }

  public SignUpVerificationResult verifySignUpOtp(String email, String otp) {
    IdentityAccount account =
        directoryAdapter
            .findByEmail(email)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

    if (STATUS_LOCKED.equalsIgnoreCase(account.status())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is locked.");
    }
    if (STATUS_INACTIVE.equalsIgnoreCase(account.status())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is inactive.");
    }
    if (STATUS_ACTIVE.equalsIgnoreCase(account.status())) {
      return new SignUpVerificationResult("Already verified", toAuthUser(account));
    }

    otpService.verifyOtp(email, OtpPurpose.REGISTER, otp);
    directoryAdapter.activateByActorId(account.actorId());
    IdentityAccount activated = directoryAdapter.findByActorId(account.actorId()).orElseThrow();
    return new SignUpVerificationResult("Verified", toAuthUser(activated));
  }

  public IdentityOtpService.OtpChallenge requestPasswordReset(String email) {
    if (!allowPasswordResetRequest(email)) {
      return new IdentityOtpService.OtpChallenge("", otpService.expirySeconds());
    }

    IdentityAccount account = directoryAdapter.findByEmail(email).orElse(null);
    if (account == null || !isActive(account)) {
      return new IdentityOtpService.OtpChallenge("", otpService.expirySeconds());
    }

    try {
      return otpService.sendOtp(email, account.actorId(), OtpPurpose.RESET_PASSWORD, true);
    } catch (ResponseStatusException exception) {
      if (exception.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
        return new IdentityOtpService.OtpChallenge("", otpService.expirySeconds());
      }
      throw exception;
    }
  }

  public PasswordResetChallenge verifyPasswordResetOtp(String email, String otp) {
    otpService.verifyOtp(email, OtpPurpose.RESET_PASSWORD, otp);
    IdentityAccount account =
        directoryAdapter
            .findByEmail(email)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP is invalid."));

    String token = resetTokenService.issueToken(account.actorId(), account.email());
    return new PasswordResetChallenge(token, resetTokenService.validSeconds());
  }

  public void resetPassword(String tempResetToken, String newPassword) {
    IdentityResetTokenService.ResetTokenPayload payload =
        resetTokenService.verifyToken(tempResetToken);
    IdentityAccount account =
        directoryAdapter
            .findByActorId(payload.actorId())
            .or(() -> directoryAdapter.findByEmail(payload.email()))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Reset token is invalid."));

    directoryAdapter.updatePasswordHash(account.actorId(), passwordEncoder.encode(newPassword));
    directoryAdapter.clearMustChangePassword(account.actorId(), LocalDateTime.now(clock));
    resetTokenService.invalidate(payload);
  }

  public void completeFirstLoginPasswordChange(
      String actorId, String currentPassword, String newPassword) {
    IdentityAccount account =
        directoryAdapter
            .findByActorId(actorId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Current account is not available."));

    if (!account.mustChangePassword()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "First-login password reset is not required.");
    }

    if (account.password() == null || account.password().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Current password cannot be verified.");
    }

    if (!passwordEncoder.matches(currentPassword, account.password())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
    }

    if (passwordEncoder.matches(newPassword, account.password())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "New password must be different from current password.");
    }

    directoryAdapter.updatePasswordAndCompleteFirstLogin(
        actorId, passwordEncoder.encode(newPassword), LocalDateTime.now(clock));
  }

  private static AuthUserView toAuthUser(IdentityAccount account) {
    return new AuthUserView(
        account.actorId(),
        account.principal(),
        account.fullName(),
        account.email(),
        account.role(),
        account.mustChangePassword());
  }

  private static boolean isPending(IdentityAccount account) {
    return STATUS_PENDING_VERIFICATION.equalsIgnoreCase(account.status());
  }

  private static boolean isActive(IdentityAccount account) {
    return STATUS_ACTIVE.equalsIgnoreCase(account.status()) && account.active();
  }

  private boolean allowPasswordResetRequest(String email) {
    if (passwordResetRateLimitMax <= 0 || passwordResetRateLimitWindowMinutes <= 0) {
      return true;
    }

    LocalDateTime now = LocalDateTime.now(clock);
    String key = normalizeRateLimitKey(email);
    LocalDateTime windowExpiresAt = now.plusMinutes(passwordResetRateLimitWindowMinutes);
    AtomicBoolean allowed = new AtomicBoolean(true);

    passwordResetRateLimits.compute(
        key,
        (ignored, existing) -> {
          if (existing == null || !existing.windowExpiresAt().isAfter(now)) {
            return new PasswordResetRateWindow(1, windowExpiresAt);
          }
          if (existing.requestCount() >= passwordResetRateLimitMax) {
            allowed.set(false);
            return existing;
          }
          return new PasswordResetRateWindow(
              existing.requestCount() + 1, existing.windowExpiresAt());
        });

    cleanupExpiredPasswordResetRateWindows(now);
    return allowed.get();
  }

  private void cleanupExpiredPasswordResetRateWindows(LocalDateTime now) {
    passwordResetRateLimits
        .entrySet()
        .removeIf(entry -> !entry.getValue().windowExpiresAt().isAfter(now));
  }

  private static String normalizeRateLimitKey(String email) {
    if (email == null || email.isBlank()) {
      return "_blank_";
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }

  public record SignUpCommand(
      String username, String email, String password, String fullName, String roleCode) {}

  public record SignUpVerificationResult(String message, AuthUserView user) {}

  public record PasswordResetChallenge(String tempResetToken, long expiresInSeconds) {}

  private record PasswordResetRateWindow(int requestCount, LocalDateTime windowExpiresAt) {}
}
