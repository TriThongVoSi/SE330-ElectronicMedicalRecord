package org.example.BenhAnDienTu.appointment.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.example.BenhAnDienTu.appointment.api.AppointmentBookingCommand;
import org.example.BenhAnDienTu.appointment.api.AppointmentListQuery;
import org.example.BenhAnDienTu.appointment.api.AppointmentPageView;
import org.example.BenhAnDienTu.appointment.api.PatientPortalAvailableSlotView;
import org.example.BenhAnDienTu.appointment.api.AppointmentView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AppointmentLedgerAdapter {

  private static final String STATUS_CANCEL = "CANCEL";

  private static final String APPOINTMENT_SELECT_SQL =
      """
      SELECT a.appointment_id, a.appointment_code, a.appointment_time, a.status, a.cancel_reason,
             a.doctor_id, d.full_name AS doctor_name,
             a.patient_id, p.full_name AS patient_name,
             a.urgency_level, a.prescription_status, a.is_followup, a.priority_score,
             a.created_at, a.updated_at
      FROM appointments a
      JOIN patients p ON p.patient_id = a.patient_id
      JOIN user_accounts d ON d.id = a.doctor_id
      """;

  private static final String APPOINTMENT_COUNT_SQL_PREFIX =
      "SELECT COUNT(*) FROM appointments a JOIN patients p ON p.patient_id = a.patient_id "
          + "JOIN user_accounts d ON d.id = a.doctor_id ";

  private static final RowMapper<AppointmentRow> APPOINTMENT_ROW_MAPPER =
      AppointmentLedgerAdapter::mapAppointmentRow;

  private final JdbcTemplate jdbcTemplate;

  public AppointmentLedgerAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public AppointmentPageView list(AppointmentListQuery query) {
    int page = normalizePage(query.page());
    int size = normalizeSize(query.size());
    int offset = (page - 1) * size;

    SqlFilter sqlFilter = buildListFilter(query);
    List<AppointmentRow> appointmentRows = fetchAppointmentRows(sqlFilter, size, offset);
    List<AppointmentView> appointmentViews = mapAppointmentViews(appointmentRows);

    long totalItems = countAppointments(sqlFilter);
    int totalPages = calculateTotalPages(totalItems, size);
    return new AppointmentPageView(appointmentViews, page, size, totalItems, totalPages);
  }

  public Optional<AppointmentView> findById(String appointmentId) {
    String sql = APPOINTMENT_SELECT_SQL + " WHERE a.appointment_id = ? LIMIT 1";
    List<AppointmentRow> rows = jdbcTemplate.query(sql, APPOINTMENT_ROW_MAPPER, appointmentId);
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    AppointmentRow row = rows.getFirst();
    List<String> serviceIds =
        findServiceIdsByAppointmentIds(List.of(row.id())).getOrDefault(row.id(), List.of());
    return Optional.of(toAppointmentView(row, serviceIds));
  }

  public boolean hasAllServiceIds(List<String> serviceIds) {
    if (serviceIds == null || serviceIds.isEmpty()) {
      return true;
    }

    String placeholders = String.join(", ", Collections.nCopies(serviceIds.size(), "?"));
    String sql =
        "SELECT COUNT(*) FROM services WHERE is_active = TRUE AND id IN (" + placeholders + ")";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, serviceIds.toArray());
    return count != null && count == serviceIds.size();
  }

  public List<PatientPortalAvailableSlotView> listAvailableSlots(
      String doctorId, LocalDate fromDate, LocalDate toDate) {
    String sql =
        """
        SELECT slot_id, doctor_id, slot_date, slot_time, duration_minutes, is_booked
        FROM available_slots
        WHERE doctor_id = ?
          AND slot_date BETWEEN ? AND ?
        ORDER BY slot_date ASC, slot_time ASC
        """;

    return jdbcTemplate.query(
        sql,
        (resultSet, rowNum) -> {
          LocalDate slotDate = resultSet.getDate("slot_date").toLocalDate();
          LocalTime slotTime = resultSet.getTime("slot_time").toLocalTime();
          LocalDateTime slotDateTime = LocalDateTime.of(slotDate, slotTime);
          return new PatientPortalAvailableSlotView(
              resultSet.getString("slot_id"),
              resultSet.getString("doctor_id"),
              slotDateTime.atZone(ZoneId.systemDefault()).toInstant(),
              resultSet.getInt("duration_minutes"),
              resultSet.getBoolean("is_booked"));
        },
        doctorId,
        fromDate,
        toDate);
  }

  public boolean isSlotAvailable(String doctorId, java.time.Instant appointmentTime) {
    LocalDate slotDate = LocalDateTime.ofInstant(appointmentTime, ZoneId.systemDefault()).toLocalDate();
    LocalTime slotTime = LocalDateTime.ofInstant(appointmentTime, ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);

    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM available_slots
            WHERE doctor_id = ?
              AND slot_date = ?
              AND slot_time = ?
              AND is_booked = FALSE
            """,
            Integer.class,
            doctorId,
            slotDate,
            slotTime);

    return count != null && count > 0;
  }

  public boolean isSlotAvailableForAppointment(
      String doctorId, java.time.Instant appointmentTime, String appointmentId) {
    LocalDate slotDate = LocalDateTime.ofInstant(appointmentTime, ZoneId.systemDefault()).toLocalDate();
    LocalTime slotTime = LocalDateTime.ofInstant(appointmentTime, ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);

    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM available_slots
            WHERE doctor_id = ?
              AND slot_date = ?
              AND slot_time = ?
              AND (is_booked = FALSE OR appointment_id = ?)
            """,
            Integer.class,
            doctorId,
            slotDate,
            slotTime,
            appointmentId);

    return count != null && count > 0;
  }

  @Transactional
  public AppointmentView create(AppointmentBookingCommand command) {
    String appointmentId = UUID.randomUUID().toString();
    String sql =
        """
        INSERT INTO appointments (
            appointment_id, appointment_code, appointment_time, status, cancel_reason,
            doctor_id, patient_id, urgency_level, prescription_status, is_followup, priority_score
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    jdbcTemplate.update(
        sql,
        appointmentId,
        command.appointmentCode(),
        command.appointmentTime(),
        command.status(),
        command.cancelReason(),
        command.doctorId(),
        command.patientId(),
        command.urgencyLevel(),
        command.prescriptionStatus(),
        command.isFollowup(),
        command.priorityScore());

    syncAppointmentServices(appointmentId, command.serviceIds());
    syncAvailableSlot(appointmentId, command.status());

    return findById(appointmentId).orElseThrow();
  }

  @Transactional
  public Optional<AppointmentView> update(String appointmentId, AppointmentBookingCommand command) {
    String sql =
        """
        UPDATE appointments
        SET appointment_code = ?,
            appointment_time = ?,
            status = ?,
            cancel_reason = ?,
            doctor_id = ?,
            patient_id = ?,
            urgency_level = ?,
            prescription_status = ?,
            is_followup = ?,
            priority_score = ?
        WHERE appointment_id = ?
        """;
    int updated =
        jdbcTemplate.update(
            sql,
            command.appointmentCode(),
            command.appointmentTime(),
            command.status(),
            command.cancelReason(),
            command.doctorId(),
            command.patientId(),
            command.urgencyLevel(),
            command.prescriptionStatus(),
            command.isFollowup(),
            command.priorityScore(),
            appointmentId);

    if (updated == 0) {
      return Optional.empty();
    }

    syncAppointmentServices(appointmentId, command.serviceIds());
    syncAvailableSlot(appointmentId, command.status());

    return findById(appointmentId);
  }

  private SqlFilter buildListFilter(AppointmentListQuery query) {
    // Keep SQL filters explicit to preserve API query behavior while making query assembly
    // readable.
    StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
    List<Object> queryArguments = new ArrayList<>();

    if (hasText(query.search())) {
      String searchPattern = "%" + query.search().trim() + "%";
      whereClause.append(
          " AND (a.appointment_code LIKE ? OR p.full_name LIKE ? OR d.full_name LIKE ?) ");
      queryArguments.add(searchPattern);
      queryArguments.add(searchPattern);
      queryArguments.add(searchPattern);
    }

    if (hasText(query.status())) {
      whereClause.append(" AND a.status = ? ");
      queryArguments.add(query.status().trim());
    }

    if (hasText(query.doctorId())) {
      whereClause.append(" AND a.doctor_id = ? ");
      queryArguments.add(query.doctorId().trim());
    }

    if (hasText(query.patientId())) {
      whereClause.append(" AND a.patient_id = ? ");
      queryArguments.add(query.patientId().trim());
    }

    LocalDate appointmentDate = query.date();
    if (appointmentDate != null) {
      whereClause.append(" AND DATE(a.appointment_time) = ? ");
      queryArguments.add(appointmentDate);
    }

    return new SqlFilter(whereClause.toString(), List.copyOf(queryArguments));
  }

  private List<AppointmentRow> fetchAppointmentRows(SqlFilter sqlFilter, int pageSize, int offset) {
    String dataSql =
        sqlFilter.decorateSelectSql(APPOINTMENT_SELECT_SQL)
            + " ORDER BY a.appointment_time DESC LIMIT ? OFFSET ?";
    List<Object> dataArguments = new ArrayList<>(sqlFilter.queryArguments());
    dataArguments.add(pageSize);
    dataArguments.add(offset);
    return jdbcTemplate.query(dataSql, APPOINTMENT_ROW_MAPPER, dataArguments.toArray());
  }

  private List<AppointmentView> mapAppointmentViews(List<AppointmentRow> appointmentRows) {
    List<String> appointmentIds = appointmentRows.stream().map(AppointmentRow::id).toList();
    Map<String, List<String>> serviceIdsByAppointmentId =
        findServiceIdsByAppointmentIds(appointmentIds);

    return appointmentRows.stream()
        .map(
            row ->
                toAppointmentView(row, serviceIdsByAppointmentId.getOrDefault(row.id(), List.of())))
        .toList();
  }

  private long countAppointments(SqlFilter sqlFilter) {
    String countSql = sqlFilter.decorateSelectSql(APPOINTMENT_COUNT_SQL_PREFIX);
    Long totalItems =
        jdbcTemplate.queryForObject(countSql, Long.class, sqlFilter.queryArguments().toArray());
    return totalItems == null ? 0 : totalItems;
  }

  private void syncAppointmentServices(String appointmentId, List<String> serviceIds) {
    jdbcTemplate.update("DELETE FROM appointment_services WHERE appointment_id = ?", appointmentId);
    if (serviceIds == null || serviceIds.isEmpty()) {
      return;
    }

    String sql = "INSERT INTO appointment_services (appointment_id, service_id) VALUES (?, ?)";
    for (String serviceId : serviceIds) {
      jdbcTemplate.update(sql, appointmentId, serviceId);
    }
  }

  private void syncAvailableSlot(String appointmentId, String appointmentStatus) {
    releaseSlotReservation(appointmentId);

    // Cancelled appointments intentionally release any slot reservation instead of re-booking a
    // slot.
    if (STATUS_CANCEL.equalsIgnoreCase(appointmentStatus)) {
      return;
    }

    if (tryReuseExistingSlot(appointmentId)) {
      return;
    }

    insertNewReservedSlot(appointmentId);
  }

  private boolean tryReuseExistingSlot(String appointmentId) {
    int reusedSlot =
        jdbcTemplate.update(
            """
            UPDATE available_slots s
            JOIN appointments a ON a.appointment_id = ?
            SET s.is_booked = TRUE,
                s.appointment_id = a.appointment_id,
                s.duration_minutes = 15
            WHERE s.doctor_id = a.doctor_id
              AND s.slot_date = DATE(a.appointment_time)
              AND s.slot_time = TIME(a.appointment_time)
              AND (s.is_booked = FALSE OR s.appointment_id = a.appointment_id)
            """,
            appointmentId);
    return reusedSlot > 0;
  }

  private void insertNewReservedSlot(String appointmentId) {
    jdbcTemplate.update(
        """
            INSERT INTO available_slots (
                slot_id,
                doctor_id,
                slot_date,
                slot_time,
                duration_minutes,
                is_booked,
                appointment_id
            )
            SELECT ?, a.doctor_id, DATE(a.appointment_time), TIME(a.appointment_time), 15, TRUE, a.appointment_id
            FROM appointments a
            WHERE a.appointment_id = ?
            """,
        UUID.randomUUID().toString(),
        appointmentId);
  }

  private void releaseSlotReservation(String appointmentId) {
    jdbcTemplate.update(
        """
            UPDATE available_slots
            SET is_booked = FALSE,
                appointment_id = NULL
            WHERE appointment_id = ?
            """,
        appointmentId);
  }

  private Map<String, List<String>> findServiceIdsByAppointmentIds(List<String> appointmentIds) {
    if (appointmentIds.isEmpty()) {
      return Map.of();
    }

    String placeholders = String.join(", ", Collections.nCopies(appointmentIds.size(), "?"));
    String sql =
        """
        SELECT appointment_id, service_id
        FROM appointment_services
        WHERE appointment_id IN (%s)
        ORDER BY appointment_id ASC, service_id ASC
        """
            .formatted(placeholders);

    Map<String, List<String>> servicesByAppointmentId = new HashMap<>();
    List<ServiceSelectionRow> rows =
        jdbcTemplate.query(
            sql,
            (resultSet, rowNum) ->
                new ServiceSelectionRow(
                    resultSet.getString("appointment_id"), resultSet.getString("service_id")),
            appointmentIds.toArray());
    for (ServiceSelectionRow row : rows) {
      servicesByAppointmentId
          .computeIfAbsent(row.appointmentId(), ignored -> new ArrayList<>())
          .add(row.serviceId());
    }

    return servicesByAppointmentId;
  }

  private AppointmentView toAppointmentView(AppointmentRow row, List<String> serviceIds) {
    return new AppointmentView(
        row.id(),
        row.appointmentCode(),
        row.appointmentTime(),
        row.status(),
        row.cancelReason(),
        row.doctorId(),
        row.doctorName(),
        row.patientId(),
        row.patientName(),
        row.urgencyLevel(),
        row.prescriptionStatus(),
        row.isFollowup(),
        row.priorityScore(),
        serviceIds,
        row.createdAt(),
        row.updatedAt());
  }

  private static AppointmentRow mapAppointmentRow(ResultSet resultSet, int rowNum)
      throws SQLException {
    return new AppointmentRow(
        resultSet.getString("appointment_id"),
        resultSet.getString("appointment_code"),
        resultSet.getTimestamp("appointment_time").toInstant(),
        resultSet.getString("status"),
        resultSet.getString("cancel_reason"),
        resultSet.getString("doctor_id"),
        resultSet.getString("doctor_name"),
        resultSet.getString("patient_id"),
        resultSet.getString("patient_name"),
        resultSet.getInt("urgency_level"),
        resultSet.getString("prescription_status"),
        resultSet.getBoolean("is_followup"),
        (Integer) resultSet.getObject("priority_score"),
        resultSet.getTimestamp("created_at").toInstant(),
        resultSet.getTimestamp("updated_at").toInstant());
  }

  private static boolean hasText(String candidate) {
    return candidate != null && !candidate.isBlank();
  }

  private static int normalizePage(int page) {
    return page <= 0 ? 1 : page;
  }

  private static int normalizeSize(int size) {
    if (size <= 0) {
      return 10;
    }
    return Math.min(size, 200);
  }

  private static int calculateTotalPages(long totalItems, int size) {
    return (int) Math.max(1, Math.ceil((double) totalItems / size));
  }

  private record SqlFilter(String whereClause, List<Object> queryArguments) {
    private String decorateSelectSql(String selectSql) {
      return selectSql + whereClause;
    }
  }

  private record AppointmentRow(
      String id,
      String appointmentCode,
      java.time.Instant appointmentTime,
      String status,
      String cancelReason,
      String doctorId,
      String doctorName,
      String patientId,
      String patientName,
      int urgencyLevel,
      String prescriptionStatus,
      boolean isFollowup,
      Integer priorityScore,
      java.time.Instant createdAt,
      java.time.Instant updatedAt) {}

  private record ServiceSelectionRow(String appointmentId, String serviceId) {}
}
