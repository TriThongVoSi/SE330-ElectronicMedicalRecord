package org.example.BenhAnDienTu.identity.infrastructure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.BenhAnDienTu.identity.api.AuthLoginCommand;
import org.example.BenhAnDienTu.identity.api.AuthRefreshCommand;
import org.example.BenhAnDienTu.identity.api.AuthSessionView;
import org.example.BenhAnDienTu.identity.api.AuthUserView;
import org.example.BenhAnDienTu.identity.api.IdentityApi;
import org.example.BenhAnDienTu.identity.application.IdentityAccountApplicationService;
import org.example.BenhAnDienTu.identity.application.IdentityOtpService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class IdentityV1AuthController {

  private final IdentityApi identityApi;
  private final IdentityAccountApplicationService accountApplicationService;

  public IdentityV1AuthController(
      IdentityApi identityApi,
      IdentityAccountApplicationService accountApplicationService) {
    this.identityApi = identityApi;
    this.accountApplicationService = accountApplicationService;
  }

  @PostMapping("/sign-in")
  public AuthSessionView signIn(@Valid @RequestBody SignInRequest request) {
    return identityApi.login(new AuthLoginCommand(request.resolveIdentifier(), request.password()));
  }

  @PostMapping("/refresh")
  public AuthSessionView refresh(@Valid @RequestBody TokenRequest request) {
    return identityApi.refresh(new AuthRefreshCommand(request.resolveToken()));
  }

  @PostMapping("/sign-out")
  public ResponseEntity<Void> signOut(@Valid @RequestBody TokenRequest request) {
    identityApi.logout(request.resolveToken());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public AuthUserView me(
      @RequestHeader(name = "Authorization", required = false) String authorization) {
    String accessToken = extractBearerToken(authorization);
    return identityApi
        .findUserByAccessToken(accessToken)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token is invalid."));
  }

  @PostMapping("/introspect")
  public IntrospectResponse introspect(@Valid @RequestBody IntrospectRequest request) {
    boolean valid = identityApi.findUserByAccessToken(request.token()).isPresent();
    return new IntrospectResponse(valid);
  }

  @PostMapping("/sign-up")
  public OtpChallengeResponse signUp(@Valid @RequestBody SignUpRequest request) {
    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "PUBLIC_REGISTRATION_NOT_ALLOWED: Public self-registration is disabled. Accounts are provisioned internally.");
  }

  @PostMapping("/sign-up/verify-otp")
  public SignUpVerifyOtpResponse verifySignUpOtp(@Valid @RequestBody VerifyOtpRequest request) {
    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "PUBLIC_REGISTRATION_NOT_ALLOWED: Public self-registration is disabled. Accounts are provisioned internally.");
  }

  @PostMapping("/forgot-password")
  public OtpChallengeResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    IdentityOtpService.OtpChallenge challenge =
        accountApplicationService.requestPasswordReset(request.email());
    return new OtpChallengeResponse(
        "If account exists, OTP has been sent.",
        "VERIFY_OTP",
        challenge.emailMasked(),
        challenge.expiresInSeconds());
  }

  @PostMapping("/forgot-password/verify-otp")
  public ForgotPasswordVerifyOtpResponse verifyForgotPasswordOtp(
      @Valid @RequestBody VerifyOtpRequest request) {
    IdentityAccountApplicationService.PasswordResetChallenge challenge =
        accountApplicationService.verifyPasswordResetOtp(request.email(), request.otp());
    return new ForgotPasswordVerifyOtpResponse(
        challenge.tempResetToken(), challenge.expiresInSeconds());
  }

  @PostMapping("/forgot-password/reset")
  public ResetPasswordResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    accountApplicationService.resetPassword(request.tempResetToken(), request.newPassword());
    return new ResetPasswordResponse("Password updated successfully.");
  }

  @PostMapping("/first-login/change-password")
  public ResponseEntity<Void> changeFirstLoginPassword(
      @RequestAttribute(name = "actorId") String actorId,
      @Valid @RequestBody FirstLoginPasswordChangeRequest request) {
    accountApplicationService.completeFirstLoginPasswordChange(
        actorId, request.currentPassword(), request.newPassword());
    return ResponseEntity.noContent().build();
  }

  private static String extractBearerToken(String authorization) {
    if (authorization == null || authorization.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header.");
    }
    if (!authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Authorization header must use Bearer token.");
    }
    String token = authorization.substring("Bearer ".length()).trim();
    if (token.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token is missing.");
    }
    return token;
  }

  public record SignInRequest(
      String identifier, String username, String email, @NotBlank String password) {

    String resolveIdentifier() {
      if (identifier != null && !identifier.isBlank()) {
        return identifier.trim();
      }
      if (username != null && !username.isBlank()) {
        return username.trim();
      }
      if (email != null && !email.isBlank()) {
        return email.trim();
      }
      return "";
    }
  }

  public record TokenRequest(String refreshToken, String token) {

    String resolveToken() {
      if (refreshToken != null && !refreshToken.isBlank()) {
        return refreshToken.trim();
      }
      if (token != null && !token.isBlank()) {
        return token.trim();
      }
      return "";
    }
  }

  public record IntrospectRequest(@NotBlank String token) {}

  public record IntrospectResponse(boolean valid) {}

  public record SignUpRequest(
      @NotBlank @Size(min = 3, max = 50) String username,
      @NotBlank @Email String email,
      @NotBlank @Size(min = 6, max = 128) String password,
      String fullName,
      String role) {}

  public record VerifyOtpRequest(
      @NotBlank @Email String email, @NotBlank @Pattern(regexp = "\\d{6}") String otp) {}

  public record ForgotPasswordRequest(@NotBlank @Email String email) {}

  public record ResetPasswordRequest(
      @NotBlank String tempResetToken, @NotBlank @Size(min = 6, max = 128) String newPassword) {}

  public record FirstLoginPasswordChangeRequest(
      @NotBlank @Size(min = 6, max = 128) String currentPassword,
      @NotBlank @Size(min = 8, max = 128) String newPassword) {}

  public record OtpChallengeResponse(
      String message, String nextStep, String emailMasked, long expiresInSeconds) {}

  public record SignUpVerifyOtpResponse(String message, AuthUserView user) {}

  public record ForgotPasswordVerifyOtpResponse(String tempResetToken, long expiresInSeconds) {}

  public record ResetPasswordResponse(String message) {}
}
