package org.example.BenhAnDienTu.staff.infrastructure;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.BenhAnDienTu.staff.api.StaffDoctorCreateCommand;
import org.example.BenhAnDienTu.staff.api.StaffDoctorUpdateCommand;
import org.example.BenhAnDienTu.staff.api.StaffMemberView;
import org.example.BenhAnDienTu.staff.api.StaffProfileUpdateCommand;
import org.example.BenhAnDienTu.staff.api.StaffProfileView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class StaffDirectoryAdapter {

  private static final String ROLE_DOCTOR = "DOCTOR";
  private static final String STATUS_ACTIVE = "ACTIVE";
  private static final String STATUS_INACTIVE = "INACTIVE";

  private static final RowMapper<StaffMemberView> STAFF_ROW_MAPPER =
      (resultSet, rowNum) ->
          new StaffMemberView(
              resultSet.getString("id"),
              resultSet.getString("full_name"),
              resultSet.getString("email"),
              resultSet.getString("phone"),
              resultSet.getString("role"));

  private static final RowMapper<StaffProfileView> PROFILE_ROW_MAPPER =
      (resultSet, rowNum) ->
          new StaffProfileView(
              resultSet.getString("id"),
              resultSet.getString("username"),
              resultSet.getString("full_name"),
              resultSet.getString("email"),
              resultSet.getString("phone"),
              resultSet.getString("gender"),
              resultSet.getString("address"),
              resultSet.getString("role"),
              resultSet.getString("status"),
              resultSet.getBoolean("is_active"),
              resultSet.getBoolean("is_confirmed"),
              resultSet.getString("service_id"));

  private final JdbcTemplate jdbcTemplate;

  public StaffDirectoryAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<StaffMemberView> findById(String staffId) {
    String sql =
        """
        SELECT u.id, u.full_name, u.email, d.phone, u.role
        FROM user_accounts u
        JOIN doctors d ON d.doctor_id = u.id
        WHERE u.id = ?
        LIMIT 1
        """;

    List<StaffMemberView> rows = jdbcTemplate.query(sql, STAFF_ROW_MAPPER, staffId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  public List<StaffMemberView> findDoctors() {
    String sql =
        """
        SELECT u.id, u.full_name, u.email, d.phone, u.role
        FROM user_accounts u
        JOIN doctors d ON d.doctor_id = u.id
        WHERE u.role = 'DOCTOR' AND u.is_active = TRUE
        ORDER BY u.full_name ASC
        """;
    return jdbcTemplate.query(sql, STAFF_ROW_MAPPER);
  }

  public Optional<StaffProfileView> findDoctorProfileById(String doctorId) {
    return queryDoctorProfile(doctorId);
  }

  public Optional<StaffProfileView> findDoctorProfileByActorId(String actorId) {
    return queryDoctorProfile(actorId);
  }

  public StaffProfileView createDoctor(String passwordHash, StaffDoctorCreateCommand command) {
    String doctorId = UUID.randomUUID().toString();
    insertDoctorAccount(doctorId, passwordHash, command);
    insertDoctorProfile(doctorId, command);

    ensureDoctorRoleAssigned(doctorId);
    return findDoctorProfileById(doctorId).orElseThrow();
  }

  public Optional<StaffProfileView> updateDoctor(
      String doctorId, StaffDoctorUpdateCommand command) {
    int updatedAccount = updateDoctorAccount(doctorId, command);

    if (updatedAccount == 0) {
      return Optional.empty();
    }

    updateDoctorProfile(doctorId, command);

    return findDoctorProfileById(doctorId);
  }

  public boolean deactivateDoctor(String doctorId) {
    int updated =
        jdbcTemplate.update(
            """
            UPDATE user_accounts u
            JOIN doctors d ON d.doctor_id = u.id
            SET u.is_active = FALSE,
                u.status = ?
            WHERE u.id = ?
            """,
            STATUS_INACTIVE,
            doctorId);
    return updated > 0;
  }

  public Optional<StaffProfileView> updateProfile(
      String actorId, StaffProfileUpdateCommand command) {
    int updated =
        jdbcTemplate.update(
            """
            UPDATE user_accounts u
            JOIN doctors d ON d.doctor_id = u.id
            SET u.full_name = ?,
                u.email = ?,
                u.gender = ?,
                d.phone = ?,
                d.address = ?
            WHERE u.id = ?
            """,
            normalizeRequired(command.fullName()),
            normalizeRequired(command.email()),
            normalizeNullable(command.gender()),
            normalizeNullable(command.phone()),
            normalizeNullable(command.address()),
            actorId);

    if (updated == 0) {
      return Optional.empty();
    }
    return findDoctorProfileByActorId(actorId);
  }

  private Optional<StaffProfileView> queryDoctorProfile(String doctorId) {
    String sql =
        """
        SELECT u.id, u.username, u.full_name, u.email, d.phone, u.gender, d.address,
               u.role, u.status, u.is_active, d.is_confirmed, d.service_id
        FROM user_accounts u
        JOIN doctors d ON d.doctor_id = u.id
        WHERE u.id = ?
        LIMIT 1
        """;
    List<StaffProfileView> rows = jdbcTemplate.query(sql, PROFILE_ROW_MAPPER, doctorId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  private void insertDoctorAccount(
      String doctorId, String passwordHash, StaffDoctorCreateCommand command) {
    boolean active = command.active();
    jdbcTemplate.update(
        """
            INSERT INTO user_accounts (
                id, username, password, email, full_name, gender, role, is_active, status, joined_date
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
        doctorId,
        normalizeRequired(command.username()),
        passwordHash,
        normalizeRequired(command.email()),
        normalizeRequired(command.fullName()),
        normalizeNullable(command.gender()),
        ROLE_DOCTOR,
        active,
        statusForActiveFlag(active),
        Timestamp.valueOf(LocalDateTime.now()));
  }

  private void insertDoctorProfile(String doctorId, StaffDoctorCreateCommand command) {
    jdbcTemplate.update(
        """
            INSERT INTO doctors (doctor_id, phone, service_id, address, is_confirmed)
            VALUES (?, ?, ?, ?, ?)
            """,
        doctorId,
        normalizeNullable(command.phone()),
        normalizeNullable(command.serviceId()),
        normalizeNullable(command.address()),
        command.confirmed());
  }

  private int updateDoctorAccount(String doctorId, StaffDoctorUpdateCommand command) {
    boolean active = command.active();
    return jdbcTemplate.update(
        """
            UPDATE user_accounts u
            JOIN doctors d ON d.doctor_id = u.id
            SET u.full_name = ?,
                u.email = ?,
                u.gender = ?,
                u.is_active = ?,
                u.status = ?
            WHERE u.id = ?
            """,
        normalizeRequired(command.fullName()),
        normalizeRequired(command.email()),
        normalizeNullable(command.gender()),
        active,
        statusForActiveFlag(active),
        doctorId);
  }

  private void updateDoctorProfile(String doctorId, StaffDoctorUpdateCommand command) {
    jdbcTemplate.update(
        """
            UPDATE doctors
            SET phone = ?,
                service_id = ?,
                address = ?,
                is_confirmed = ?
            WHERE doctor_id = ?
            """,
        normalizeNullable(command.phone()),
        normalizeNullable(command.serviceId()),
        normalizeNullable(command.address()),
        command.confirmed(),
        doctorId);
  }

  private void ensureDoctorRoleAssigned(String doctorId) {
    // Keep user_roles in sync so doctor accounts participate in role-based policies.
    jdbcTemplate.update(
        """
            INSERT INTO user_roles (user_id, role_id)
            SELECT ?, role_id
            FROM roles
            WHERE code = ?
            LIMIT 1
            ON DUPLICATE KEY UPDATE user_id = user_id
            """,
        doctorId,
        ROLE_DOCTOR);
    jdbcTemplate.update("UPDATE user_accounts SET role = ? WHERE id = ?", ROLE_DOCTOR, doctorId);
  }

  private static String statusForActiveFlag(boolean active) {
    return active ? STATUS_ACTIVE : STATUS_INACTIVE;
  }

  private static String normalizeRequired(String value) {
    return value == null ? null : value.trim();
  }

  private static String normalizeNullable(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
