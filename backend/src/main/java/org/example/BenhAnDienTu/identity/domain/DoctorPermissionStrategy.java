package org.example.BenhAnDienTu.identity.domain;

import java.util.Set;

public class DoctorPermissionStrategy implements RolePermissionStrategy {

  private static final String ROLE_DOCTOR = "DOCTOR";
  private static final Set<String> DOCTOR_ALLOWED_PREFIXES =
      Set.of(
          "/api/dashboard",
          "/api/patients",
          "/api/appointments",
          "/api/drugs",
          "/api/prescriptions",
          "/api/notifications");
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
      return isMethod(method, "GET") || isMethod(method, "PUT");
    }

    if (path.startsWith("/api/staff/doctors")) {
      return isMethod(method, "GET");
    }

    for (String prefix : DOCTOR_ALLOWED_PREFIXES) {
      if (path.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private boolean isMethod(String actual, String expected) {
    return expected.equalsIgnoreCase(actual);
  }
}
