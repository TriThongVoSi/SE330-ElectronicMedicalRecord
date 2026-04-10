package org.example.BenhAnDienTu.identity.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.BenhAnDienTu.identity.application.RolePermissionResolver;
import org.example.BenhAnDienTu.identity.domain.IdentityAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class IdentityDirectoryAdapter {

  private static final String STATUS_ACTIVE = "ACTIVE";
  private static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";
  private static final String ROLE_ADMIN = "ADMIN";
  private static final String ROLE_DOCTOR = "DOCTOR";
  private static final String ROLE_PATIENT = "PATIENT";

  private final JdbcTemplate jdbcTemplate;
  private final RolePermissionResolver rolePermissionResolver;

  public IdentityDirectoryAdapter(
      JdbcTemplate jdbcTemplate, RolePermissionResolver rolePermissionResolver) {
    this.jdbcTemplate = jdbcTemplate;
    this.rolePermissionResolver = rolePermissionResolver;
  }

  public Optional<IdentityAccount> findByIdentifier(String identifier) {
    String sql =
        """
        SELECT id, username, full_name, email, role, password, status, is_active,
               must_change_password, linked_patient_id
        FROM user_accounts
        WHERE username = ? OR email = ?
        LIMIT 1
        """;
    return querySingle(sql, identifier, identifier);
  }

  public Optional<IdentityAccount> findByPrincipal(String principal) {
    String sql =
        """
        SELECT id, username, full_name, email, role, password, status, is_active,
               must_change_password, linked_patient_id
        FROM user_accounts
        WHERE username = ?
        LIMIT 1
        """;
    return querySingle(sql, principal);
  }

  public Optional<IdentityAccount> findByEmail(String email) {
    String sql =
        """
        SELECT id, username, full_name, email, role, password, status, is_active,
               must_change_password, linked_patient_id
        FROM user_accounts
        WHERE email = ?
        LIMIT 1
        """;
    return querySingle(sql, email);
  }

  public Optional<IdentityAccount> findByActorId(String actorId) {
    String sql =
        """
        SELECT id, username, full_name, email, role, password, status, is_active,
               must_change_password, linked_patient_id
        FROM user_accounts
        WHERE id = ?
        LIMIT 1
        """;
    return querySingle(sql, actorId);
  }

  public Optional<String> findActorIdByEmail(String email) {
    String sql =
        """
        SELECT id
        FROM user_accounts
        WHERE email = ?
        LIMIT 1
        """;
    List<String> rows = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("id"), email);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  public Optional<String> findActorIdByLinkedPatientId(String patientId) {
    String sql =
        """
        SELECT id
        FROM user_accounts
        WHERE linked_patient_id = ?
        LIMIT 1
        """;
    List<String> rows = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("id"), patientId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  public Optional<String> findLinkedPatientIdByActorId(String actorId) {
    String sql =
        """
        SELECT linked_patient_id
        FROM user_accounts
        WHERE id = ?
        LIMIT 1
        """;
    List<String> rows =
        jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("linked_patient_id"), actorId);
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    String linkedPatientId = rows.getFirst();
    return linkedPatientId == null || linkedPatientId.isBlank()
        ? Optional.empty()
        : Optional.of(linkedPatientId);
  }

  public boolean existsByUsername(String username) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_accounts WHERE username = ?", Integer.class, username);
    return count != null && count > 0;
  }

  public boolean existsByEmail(String email) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_accounts WHERE email = ?", Integer.class, email);
    return count != null && count > 0;
  }

  public boolean hasRoleCode(String roleCode) {
    if (roleCode == null || roleCode.isBlank()) {
      return false;
    }
    return roleExists(roleCode.trim().toUpperCase());
  }

  public IdentityAccount createUser(CreateUserCommand command) {
    String actorId = command.actorId() == null ? UUID.randomUUID().toString() : command.actorId();
    String roleCode = resolveRoleCode(command.roleCode());
    String statusCode = resolveStatusCode(command.status());
    String fullName =
        command.fullName() == null || command.fullName().isBlank()
            ? command.username()
            : command.fullName();
    boolean active = STATUS_ACTIVE.equals(statusCode);

    LocalDateTime joinedDate =
        command.joinedDate() == null ? LocalDateTime.now() : command.joinedDate();

    jdbcTemplate.update(
        """
            INSERT INTO user_accounts (
                id, username, password, email, full_name, gender, role, is_active, status, joined_date,
                must_change_password, temp_password_issued_at, linked_patient_id
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
        actorId,
        command.username(),
        command.passwordHash(),
        command.email(),
        fullName,
        command.gender(),
        roleCode,
        active,
        statusCode,
        Timestamp.valueOf(joinedDate),
        command.mustChangePassword(),
        command.tempPasswordIssuedAt() == null ? null : Timestamp.valueOf(command.tempPasswordIssuedAt()),
        command.linkedPatientId());

    ensureRoleAssigned(actorId, roleCode);
    if (active) {
      ensureDomainProfile(actorId, roleCode);
    }

    return findByActorId(actorId).orElseThrow();
  }

  public void activateByActorId(String actorId) {
    jdbcTemplate.update(
        """
            UPDATE user_accounts
            SET status = ?, is_active = TRUE
            WHERE id = ?
            """,
        STATUS_ACTIVE,
        actorId);

    findByActorId(actorId).ifPresent(account -> ensureDomainProfile(actorId, account.role()));
  }

  public void updatePasswordHash(String actorId, String passwordHash) {
    jdbcTemplate.update(
        """
            UPDATE user_accounts
            SET password = ?
            WHERE id = ?
            """,
        passwordHash,
        actorId);
  }

  public void markMustChangePassword(String actorId, LocalDateTime issuedAt) {
    jdbcTemplate.update(
        """
            UPDATE user_accounts
            SET must_change_password = TRUE,
                temp_password_issued_at = ?,
                first_login_completed_at = NULL
            WHERE id = ?
            """,
        Timestamp.valueOf(issuedAt),
        actorId);
  }

  public void clearMustChangePassword(String actorId, LocalDateTime changedAt) {
    jdbcTemplate.update(
        """
            UPDATE user_accounts
            SET must_change_password = FALSE,
                temp_password_issued_at = NULL,
                password_changed_at = ?,
                first_login_completed_at = COALESCE(first_login_completed_at, ?)
            WHERE id = ?
            """,
        Timestamp.valueOf(changedAt),
        Timestamp.valueOf(changedAt),
        actorId);
  }

  public void updatePasswordAndCompleteFirstLogin(
      String actorId, String passwordHash, LocalDateTime changedAt) {
    jdbcTemplate.update(
        """
            UPDATE user_accounts
            SET password = ?,
                must_change_password = FALSE,
                temp_password_issued_at = NULL,
                password_changed_at = ?,
                first_login_completed_at = COALESCE(first_login_completed_at, ?)
            WHERE id = ?
            """,
        passwordHash,
        Timestamp.valueOf(changedAt),
        Timestamp.valueOf(changedAt),
        actorId);
  }

  public void linkPatientAccount(String actorId, String patientId) {
    jdbcTemplate.update(
        """
            UPDATE user_accounts
            SET linked_patient_id = ?
            WHERE id = ?
            """,
        patientId,
        actorId);
  }

  public void ensureRoleAssigned(String actorId, String roleCode) {
    String resolvedRoleCode = resolveRoleCode(roleCode);
    int inserted =
        jdbcTemplate.update(
            """
            INSERT INTO user_roles (user_id, role_id)
            SELECT ?, role_id
            FROM roles
            WHERE code = ?
            LIMIT 1
            ON DUPLICATE KEY UPDATE user_id = user_id
            """,
            actorId,
            resolvedRoleCode);

    if (inserted == 0 && !ROLE_PATIENT.equals(resolvedRoleCode) && roleExists(ROLE_PATIENT)) {
      jdbcTemplate.update(
          """
              INSERT INTO user_roles (user_id, role_id)
              SELECT ?, role_id
              FROM roles
              WHERE code = ?
              LIMIT 1
              ON DUPLICATE KEY UPDATE user_id = user_id
              """,
          actorId,
          ROLE_PATIENT);
      resolvedRoleCode = ROLE_PATIENT;
    }

    jdbcTemplate.update(
        "UPDATE user_accounts SET role = ? WHERE id = ?", resolvedRoleCode, actorId);
  }

  private Optional<IdentityAccount> querySingle(String sql, Object... parameters) {
    List<IdentityAccount> rows = jdbcTemplate.query(sql, this::mapAccount, parameters);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  private IdentityAccount mapAccount(ResultSet resultSet, int rowNum) throws SQLException {
    String role = resultSet.getString("role");
    String status = resultSet.getString("status");
    boolean active =
        STATUS_ACTIVE.equalsIgnoreCase(status)
            && Boolean.TRUE.equals(resultSet.getBoolean("is_active"));
    boolean mustChangePassword = resultSet.getBoolean("must_change_password");
    return new IdentityAccount(
        resultSet.getString("id"),
        resultSet.getString("username"),
        resultSet.getString("full_name"),
        resultSet.getString("email"),
        role,
        resultSet.getString("password"),
        active,
        status,
        mustChangePassword,
        resultSet.getString("linked_patient_id"),
        rolePermissionResolver.permissionsForRole(role));
  }

  private String resolveRoleCode(String roleCode) {
    String normalized = roleCode == null ? ROLE_DOCTOR : roleCode.trim().toUpperCase();
    if (normalized.isBlank()) {
      normalized = ROLE_DOCTOR;
    }
    if (!roleExists(normalized)) {
      if (roleExists(ROLE_DOCTOR)) {
        return ROLE_DOCTOR;
      }
      if (roleExists(ROLE_PATIENT)) {
        return ROLE_PATIENT;
      }
    }
    return normalized;
  }

  private String resolveStatusCode(String statusCode) {
    String normalized =
        statusCode == null ? STATUS_PENDING_VERIFICATION : statusCode.trim().toUpperCase();
    if (normalized.isBlank()) {
      return STATUS_PENDING_VERIFICATION;
    }
    return normalized;
  }

  private boolean roleExists(String roleCode) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM roles WHERE code = ?", Integer.class, roleCode);
    return count != null && count > 0;
  }

  private void ensureDomainProfile(String actorId, String roleCode) {
    String normalizedRole = roleCode == null ? "" : roleCode.trim().toUpperCase();
    if (ROLE_ADMIN.equals(normalizedRole)) {
      jdbcTemplate.update(
          """
              INSERT INTO admins (admin_id)
              VALUES (?)
              ON DUPLICATE KEY UPDATE admin_id = admin_id
              """,
          actorId);
      return;
    }
    if (ROLE_DOCTOR.equals(normalizedRole)) {
      jdbcTemplate.update(
          """
              INSERT INTO doctors (doctor_id, phone, service_id, address, is_confirmed)
              VALUES (?, NULL, NULL, NULL, FALSE)
              ON DUPLICATE KEY UPDATE doctor_id = doctor_id
              """,
          actorId);
    }
  }

  public record CreateUserCommand(
      String actorId,
      String username,
      String email,
      String passwordHash,
      String fullName,
      String gender,
      String roleCode,
      String status,
      LocalDateTime joinedDate,
      boolean mustChangePassword,
      LocalDateTime tempPasswordIssuedAt,
      String linkedPatientId) {

    public CreateUserCommand {
      if (username == null || username.isBlank()) {
        throw new IllegalArgumentException("username is required");
      }
      if (email == null || email.isBlank()) {
        throw new IllegalArgumentException("email is required");
      }
      if (passwordHash == null || passwordHash.isBlank()) {
        throw new IllegalArgumentException("passwordHash is required");
      }
    }

    public static CreateUserCommand pendingVerification(
        String username, String email, String passwordHash, String fullName, String roleCode) {
      return new CreateUserCommand(
          null,
          username,
          email,
          passwordHash,
          fullName,
          null,
          roleCode,
          STATUS_PENDING_VERIFICATION,
          LocalDateTime.now(),
          false,
          null,
          null);
    }

    public static CreateUserCommand activeAccount(
        String actorId,
        String username,
        String email,
        String passwordHash,
        String fullName,
        String gender,
        String roleCode,
        boolean mustChangePassword,
        LocalDateTime tempPasswordIssuedAt,
        String linkedPatientId) {
      return new CreateUserCommand(
          actorId,
          username,
          email,
          passwordHash,
          fullName,
          gender,
          roleCode,
          STATUS_ACTIVE,
          LocalDateTime.now(),
          mustChangePassword,
          tempPasswordIssuedAt,
          linkedPatientId);
    }
  }
}
