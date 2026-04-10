package org.example.BenhAnDienTu.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.example.BenhAnDienTu.identity.application.RolePermissionResolver;
import org.example.BenhAnDienTu.identity.domain.AdminPermissionStrategy;
import org.example.BenhAnDienTu.identity.domain.DefaultPermissionStrategy;
import org.example.BenhAnDienTu.identity.domain.DoctorPermissionStrategy;
import org.example.BenhAnDienTu.identity.domain.PatientPermissionStrategy;
import org.example.BenhAnDienTu.shared.logging.RequestCorrelationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiAuthorizationFilterTests {

  private final DefaultPermissionStrategy defaultPermissionStrategy =
      new DefaultPermissionStrategy();
  private final ApiAuthorizationFilter filter =
      new ApiAuthorizationFilter(
          new ObjectMapper(),
          new RolePermissionResolver(
              List.of(
                  new AdminPermissionStrategy(),
                  new DoctorPermissionStrategy(),
                  new PatientPermissionStrategy(),
                  defaultPermissionStrategy),
              defaultPermissionStrategy));

  @Test
  void adminCanAccessProtectedEndpoints() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/services");
    request.setAttribute(ApiAuthenticationFilter.ACTOR_ROLE_ATTRIBUTE, "ADMIN");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "req-admin");
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void doctorCannotAccessAdminOnlyEndpoints() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/services");
    request.setAttribute(ApiAuthenticationFilter.ACTOR_ROLE_ATTRIBUTE, "DOCTOR");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "req-doctor-denied");
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("AUTH_FORBIDDEN");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void doctorCanAccessEMRalEndpoints() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients");
    request.setAttribute(ApiAuthenticationFilter.ACTOR_ROLE_ATTRIBUTE, "DOCTOR");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "req-doctor-allowed");
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void requestWithoutRoleIsForbidden() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "req-no-role");
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("User role is missing");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void authEndpointsBypassRoleFilter() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void doctorCanAccessOwnStaffProfileEndpoints() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/staff/profile");
    request.setAttribute(ApiAuthenticationFilter.ACTOR_ROLE_ATTRIBUTE, "DOCTOR");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "req-doctor-profile");
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void patientCanAccessPatientPortalEndpoints() throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/v1/patient-portal/medical-history");
    request.setAttribute(ApiAuthenticationFilter.ACTOR_ROLE_ATTRIBUTE, "PATIENT");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "req-patient-allowed");
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void patientCannotAccessAdminDoctorBusinessEndpoints() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients");
    request.setAttribute(ApiAuthenticationFilter.ACTOR_ROLE_ATTRIBUTE, "PATIENT");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "req-patient-denied");
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(chain.getRequest()).isNull();
  }
}
