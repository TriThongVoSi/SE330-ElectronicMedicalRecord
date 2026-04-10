package org.example.BenhAnDienTu.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.BenhAnDienTu.identity.api.AuthLoginCommand;
import org.example.BenhAnDienTu.identity.api.AuthSessionView;
import org.example.BenhAnDienTu.identity.api.AuthUserView;
import org.example.BenhAnDienTu.identity.api.IdentityApi;
import org.example.BenhAnDienTu.identity.application.IdentityAccountApplicationService;
import org.example.BenhAnDienTu.identity.application.IdentityOtpService;
import org.example.BenhAnDienTu.shared.error.GlobalExceptionHandler;
import org.example.BenhAnDienTu.shared.logging.RequestCorrelationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class IdentityV1AuthControllerTests {

  @Mock private IdentityApi identityApi;
  @Mock private IdentityAccountApplicationService accountApplicationService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    IdentityV1AuthController controller =
        new IdentityV1AuthController(identityApi, accountApplicationService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new RequestCorrelationFilter())
            .build();
  }

  @Test
  void signInShouldResolveIdentifierFromUsername() throws Exception {
    AuthUserView user =
        new AuthUserView(
            "doctor-1", "doctor.local", "Doctor Local", "doctor.local@EMR.dev", "DOCTOR", false);
    when(identityApi.login(any(AuthLoginCommand.class)))
        .thenReturn(new AuthSessionView("access-token", "refresh-token", user));

    mockMvc
        .perform(
            post("/api/v1/auth/sign-in")
                .contentType("application/json")
                .content(
                    """
                        {
                          "username": "doctor.local",
                          "password": "doctor123"
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access-token"))
        .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
        .andExpect(jsonPath("$.user.username").value("doctor.local"));

    ArgumentCaptor<AuthLoginCommand> loginCaptor = ArgumentCaptor.forClass(AuthLoginCommand.class);
    verify(identityApi).login(loginCaptor.capture());
    assertThat(loginCaptor.getValue().identifier()).isEqualTo("doctor.local");
  }

  @Test
  void signOutShouldResolveTokenFallbackAndReturnNoContent() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/sign-out")
                .contentType("application/json")
                .content(
                    """
                        {
                          "token": "refresh-token"
                        }
                        """))
        .andExpect(status().isNoContent());

    verify(identityApi).logout("refresh-token");
  }

  @Test
  void meShouldReturnUnauthorizedWhenAuthorizationHeaderMissing() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.message").value("Missing Authorization header."));
  }

  @Test
  void signUpShouldReturnFeatureDisabledError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/sign-up")
                .contentType("application/json")
                .content(
                    """
                        {
                          "username": "doctor.local",
                          "email": "doctor.local@EMR.dev",
                          "password": "doctor123",
                          "fullName": "Doctor Local",
                          "role": "DOCTOR"
                        }
                        """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("PUBLIC_REGISTRATION_NOT_ALLOWED")));
  }

  @Test
  void verifyForgotPasswordOtpShouldReturnResetChallenge() throws Exception {
    when(accountApplicationService.verifyPasswordResetOtp("doctor.local@EMR.dev", "123456"))
        .thenReturn(
            new IdentityAccountApplicationService.PasswordResetChallenge("temp-reset-token", 600));

    mockMvc
        .perform(
            post("/api/v1/auth/forgot-password/verify-otp")
                .contentType("application/json")
                .content(
                    """
                        {
                          "email": "doctor.local@EMR.dev",
                          "otp": "123456"
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tempResetToken").value("temp-reset-token"))
        .andExpect(jsonPath("$.expiresInSeconds").value(600));
  }

  @Test
  void verifySignUpOtpShouldReturnFeatureDisabledError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/sign-up/verify-otp")
                .contentType("application/json")
                .content(
                    """
                        {
                          "email": "doctor.local@EMR.dev",
                          "otp": "123456"
                        }
                        """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("PUBLIC_REGISTRATION_NOT_ALLOWED")));
  }

  @Test
  void forgotPasswordShouldReturnOtpChallengeResponse() throws Exception {
    when(accountApplicationService.requestPasswordReset("doctor.local@EMR.dev"))
        .thenReturn(new IdentityOtpService.OtpChallenge("d***l@EMR.dev", 300));

    mockMvc
        .perform(
            post("/api/v1/auth/forgot-password")
                .contentType("application/json")
                .content(
                    """
                        {
                          "email": "doctor.local@EMR.dev"
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("If account exists, OTP has been sent."))
        .andExpect(jsonPath("$.nextStep").value("VERIFY_OTP"))
        .andExpect(jsonPath("$.emailMasked").value("d***l@EMR.dev"))
        .andExpect(jsonPath("$.expiresInSeconds").value(300));
  }

  @Test
  void firstLoginChangePasswordShouldDelegateToAccountService() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/first-login/change-password")
                .requestAttr("actorId", "patient-actor-1")
                .contentType("application/json")
                .content(
                    """
                        {
                          "currentPassword": "temp-password",
                          "newPassword": "new-password-123"
                        }
                        """))
        .andExpect(status().isNoContent());

    verify(accountApplicationService)
        .completeFirstLoginPasswordChange(
            "patient-actor-1", "temp-password", "new-password-123");
  }
}
