package org.example.BenhAnDienTu.identity.domain;

import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class DoctorPermissionStrategy implements RolePermissionStrategy {

  private static final String ROLE_DOCTOR = "DOCTOR";
  private static final Set<String> DOCTOR_ALLOWED_PREFIXES =
      Set.of(
          "/api/dashboard",
          "/api/patients",
          "/api/appointments",
          "/api/drugs",
          "/api/prescriptions");
  private static final Set<String> DOCTOR_PERMISSIONS =
      Set.of(
          "PATIENT_READ",
          "PATIENT_WRITE",
          "APPOINTMENT_READ",
          "PRESCRIPTION_READ",
          "PRESCRIPTION_WRITE",
          "CATALOG_READ",
          "REPORTING_READ");

  @Override
  public boolean supports(String role) {
    return ROLE_DOCTOR.equals(RolePermissionStrategy.normalizeRole(role));
  }

  @Override
  public Set<String> permissions() {
    return DOCTOR_PERMISSIONS;
  }

  @Override
  public boolean isAllowed(String method, String path) {
    if (path.startsWith("/api/staff/profile")) {
      return HttpMethod.GET.matches(method) || HttpMethod.PUT.matches(method);
    }

    if (path.startsWith("/api/staff/doctors")) {
      return HttpMethod.GET.matches(method);
    }

    for (String prefix : DOCTOR_ALLOWED_PREFIXES) {
      if (path.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
}
