package org.example.BenhAnDienTu.identity.domain;

import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class PatientPermissionStrategy implements RolePermissionStrategy {

  private static final String ROLE_PATIENT = "PATIENT";
  private static final Set<String> PATIENT_ALLOWED_PREFIXES =
      Set.of(
          "/api/v1/patient-portal/profile",
          "/api/v1/patient-portal/appointments",
          "/api/v1/patient-portal/doctors",
          "/api/v1/patient-portal/available-slots",
          "/api/v1/patient-portal/medical-history",
          "/api/v1/patient-portal/dashboard");

  private static final Set<String> PATIENT_PERMISSIONS =
      Set.of(
          "PATIENT_PORTAL_PROFILE_READ",
          "PATIENT_PORTAL_PROFILE_WRITE",
          "PATIENT_PORTAL_HISTORY_READ",
          "PATIENT_PORTAL_APPOINTMENT_READ",
          "PATIENT_PORTAL_APPOINTMENT_WRITE");

  @Override
  public boolean supports(String role) {
    return ROLE_PATIENT.equals(RolePermissionStrategy.normalizeRole(role));
  }

  @Override
  public Set<String> permissions() {
    return PATIENT_PERMISSIONS;
  }

  @Override
  public boolean isAllowed(String method, String path) {
    if (path == null || path.isBlank()) {
      return false;
    }

    if (path.startsWith("/api/v1/patient-portal/profile")) {
      return HttpMethod.GET.matches(method) || HttpMethod.PUT.matches(method);
    }

    for (String prefix : PATIENT_ALLOWED_PREFIXES) {
      if (path.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
}
