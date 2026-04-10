package org.example.BenhAnDienTu.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.example.BenhAnDienTu.identity.api.AuthUserView;
import org.example.BenhAnDienTu.identity.api.IdentityApi;
import org.example.BenhAnDienTu.shared.logging.RequestCorrelationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ApiAuthenticationFilterTests {

  @Mock private IdentityApi identityApi;

  private ApiAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new ApiAuthenticationFilter(identityApi, new ObjectMapper());
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldBypassAuthenticationForOptionsPreflight() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/dashboard/summary");
    request.addHeader("Origin", "http://localhost:5173");
    request.addHeader("Access-Control-Request-Method", "GET");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isNotNull();
    verify(identityApi, never()).findUserByAccessToken(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void shouldBypassAuthenticationForPublicApiEndpoints() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ping");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isNotNull();
    verify(identityApi, never()).findUserByAccessToken(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void shouldReturnUnauthorizedWhenBearerTokenMissing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "req-missing-token");
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("AUTH_MISSING_TOKEN");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void shouldReturnUnauthorizedWhenAccessTokenInvalid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients");
    request.addHeader("Authorization", "Bearer invalid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "req-invalid-token");
    MockFilterChain chain = new MockFilterChain();
    when(identityApi.findUserByAccessToken("invalid-token")).thenReturn(Optional.empty());

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("AUTH_INVALID_TOKEN");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void shouldSetActorAttributesWhenTokenIsValid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients");
    request.addHeader("Authorization", "Bearer access-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    when(identityApi.findUserByAccessToken("access-token"))
        .thenReturn(
            Optional.of(
                new AuthUserView(
                    "doctor-1",
                    "doctor.local",
                    "Doctor Local",
                    "doctor.local@EMR.dev",
                    "DOCTOR",
                    false)));

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isNotNull();
    assertThat(request.getAttribute(ApiAuthenticationFilter.ACTOR_ID_ATTRIBUTE))
        .isEqualTo("doctor-1");
    assertThat(request.getAttribute(ApiAuthenticationFilter.ACTOR_USERNAME_ATTRIBUTE))
        .isEqualTo("doctor.local");
    assertThat(request.getAttribute(ApiAuthenticationFilter.ACTOR_ROLE_ATTRIBUTE))
        .isEqualTo("DOCTOR");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldBlockBusinessEndpointsWhenMustChangePasswordFlagIsTrue() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients");
    request.addHeader("Authorization", "Bearer access-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    when(identityApi.findUserByAccessToken("access-token"))
        .thenReturn(
            Optional.of(
                new AuthUserView(
                    "patient-1",
                    "patient.local",
                    "Patient Local",
                    "patient.local@EMR.dev",
                    "PATIENT",
                    true)));

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("MUST_CHANGE_PASSWORD_REQUIRED");
    assertThat(chain.getRequest()).isNull();
  }
}
