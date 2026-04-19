package org.example.BenhAnDienTu.reporting.infrastructure;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.example.BenhAnDienTu.reporting.api.DashboardSummaryView;
import org.example.BenhAnDienTu.reporting.api.PatientMedicalTimelineEntryView;
import org.example.BenhAnDienTu.reporting.api.PatientMedicalTimelineView;
import org.example.BenhAnDienTu.reporting.api.PatientMedicalVisitDetailView;
import org.example.BenhAnDienTu.reporting.api.PatientMedicalVisitMedicationView;
import org.example.BenhAnDienTu.reporting.api.PatientPortalDashboardView;
import org.example.BenhAnDienTu.reporting.api.ReportingSnapshotQuery;
import org.example.BenhAnDienTu.reporting.api.UpcomingAppointmentView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReportingProjectionAdapter {

  private final JdbcTemplate jdbcTemplate;

  public ReportingProjectionAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public DashboardSummaryView loadDashboardSummary() {
    LocalDate today = LocalDate.now();

    long totalAppointments = countAppointmentsByStatus(today, null);
    long comingAppointments = countAppointmentsByStatus(today, "COMING");
    long finishedAppointments = countAppointmentsByStatus(today, "FINISH");
    long cancelledAppointments = countAppointmentsByStatus(today, "CANCEL");
    long newPatients = countNewPatients(today);

    String upcomingSql =
        """
        SELECT a.appointment_id, a.appointment_time, p.full_name AS patient_name, d.full_name AS doctor_name, a.status
        FROM appointments a
        JOIN patients p ON p.patient_id = a.patient_id
        JOIN user_accounts d ON d.id = a.doctor_id
        WHERE a.appointment_time >= NOW()
        ORDER BY a.appointment_time ASC
        LIMIT 10
        """;

    List<UpcomingAppointmentView> upcomingAppointments =
        jdbcTemplate.query(
            upcomingSql,
            (resultSet, rowNum) ->
                new UpcomingAppointmentView(
                    resultSet.getString("appointment_id"),
                    resultSet.getTimestamp("appointment_time").toInstant(),
                    resultSet.getString("patient_name"),
                    resultSet.getString("doctor_name"),
                    resultSet.getString("status")));

    return new DashboardSummaryView(
        totalAppointments,
        comingAppointments,
        finishedAppointments,
        cancelledAppointments,
        newPatients,
        upcomingAppointments);
  }

  public List<String> collectHighlights(ReportingSnapshotQuery query) {
    List<String> highlights = new ArrayList<>();
    highlights.add("Window: " + query.fromInclusive() + " -> " + query.toExclusive());
    highlights.add("Total appointments today: " + countAppointmentsByStatus(LocalDate.now(), null));
    highlights.add("New patients today: " + countNewPatients(LocalDate.now()));
    return highlights;
  }

  public Optional<PatientMedicalTimelineView> loadPatientMedicalTimeline(
      String patientId, int limit) {
    Optional<PatientSummaryRow> patientSummary = findPatientSummary(patientId);
    if (patientSummary.isEmpty()) {
      return Optional.empty();
    }

    PatientSummaryRow summaryRow = patientSummary.get();
    List<PatientMedicalTimelineEntryView> timelineEntries = findTimelineEntries(patientId, limit);
    return Optional.of(
        new PatientMedicalTimelineView(
            summaryRow.patientId(), summaryRow.patientName(), timelineEntries));
  }

  public Optional<PatientMedicalVisitDetailView> loadPatientVisitDetail(
      String patientId, String appointmentId) {
    String sql =
        """
        SELECT a.appointment_id,
               a.appointment_code,
               a.appointment_time,
               a.status AS appointment_status,
               a.doctor_id,
               d.full_name AS doctor_name,
               pr.prescription_id,
               pr.prescription_code,
               pr.status AS prescription_status,
               pr.diagnosis,
               pr.advice
        FROM appointments a
        JOIN user_accounts d ON d.id = a.doctor_id
        LEFT JOIN prescriptions pr ON pr.appointment_id = a.appointment_id
        WHERE a.patient_id = ?
          AND a.appointment_id = ?
        LIMIT 1
        """;

    List<PatientMedicalVisitDetailView> rows =
        jdbcTemplate.query(
            sql,
            (resultSet, rowNum) -> {
              String prescriptionId = resultSet.getString("prescription_id");
              List<PatientMedicalVisitMedicationView> medications =
                  prescriptionId == null ? List.of() : findVisitMedications(prescriptionId);

              return new PatientMedicalVisitDetailView(
                  resultSet.getString("appointment_id"),
                  resultSet.getString("appointment_code"),
                  resultSet.getTimestamp("appointment_time").toInstant(),
                  resultSet.getString("appointment_status"),
                  resultSet.getString("doctor_id"),
                  resultSet.getString("doctor_name"),
                  resultSet.getString("diagnosis"),
                  resultSet.getString("advice"),
                  prescriptionId,
                  resultSet.getString("prescription_code"),
                  resultSet.getString("prescription_status"),
                  medications);
            },
            patientId,
            appointmentId);

    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  public PatientPortalDashboardView loadPatientDashboard(String patientId) {
    Integer upcomingAppointments =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM appointments
            WHERE patient_id = ?
              AND appointment_time > NOW()
              AND status <> 'CANCEL'
            """,
            Integer.class,
            patientId);

    List<PatientMedicalTimelineEntryView> latestEntries = findTimelineEntries(patientId, 20);
    PatientMedicalTimelineEntryView latestVisit =
        latestEntries.isEmpty() ? null : latestEntries.getFirst();
    PatientMedicalTimelineEntryView latestPrescription =
        latestEntries.stream().filter(entry -> entry.prescriptionId() != null).findFirst().orElse(null);

    return new PatientPortalDashboardView(
        upcomingAppointments == null ? 0 : upcomingAppointments,
        latestVisit,
        latestPrescription);
  }

  private Optional<PatientSummaryRow> findPatientSummary(String patientId) {
    String sql =
        """
        SELECT patient_id, full_name
        FROM patients
        WHERE patient_id = ?
        LIMIT 1
        """;
    List<PatientSummaryRow> rows =
        jdbcTemplate.query(
            sql,
            (resultSet, rowNum) ->
                new PatientSummaryRow(
                    resultSet.getString("patient_id"), resultSet.getString("full_name")),
            patientId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  private List<PatientMedicalTimelineEntryView> findTimelineEntries(
      String patientId, int requestedLimit) {
    int limit = normalizeTimelineLimit(requestedLimit);
    String sql =
        """
        SELECT a.appointment_id,
               a.appointment_code,
               a.appointment_time,
               a.status AS appointment_status,
               a.doctor_id,
               d.full_name AS doctor_name,
               pr.prescription_id,
               pr.prescription_code,
               pr.status AS prescription_status,
               pr.created_at AS prescription_issued_at,
               pr.diagnosis,
               pr.advice
        FROM appointments a
        JOIN user_accounts d ON d.id = a.doctor_id
        LEFT JOIN prescriptions pr ON pr.appointment_id = a.appointment_id
        WHERE a.patient_id = ?
        ORDER BY a.appointment_time DESC, pr.created_at DESC
        LIMIT ?
        """;
    return jdbcTemplate.query(
        sql,
        (resultSet, rowNum) ->
            new PatientMedicalTimelineEntryView(
                resultSet.getString("appointment_id"),
                resultSet.getString("appointment_code"),
                resultSet.getTimestamp("appointment_time").toInstant(),
                resultSet.getString("appointment_status"),
                resultSet.getString("doctor_id"),
                resultSet.getString("doctor_name"),
                resultSet.getString("prescription_id"),
                resultSet.getString("prescription_code"),
                resultSet.getString("prescription_status"),
                resultSet.getTimestamp("prescription_issued_at") == null
                    ? null
                    : resultSet.getTimestamp("prescription_issued_at").toInstant(),
                resultSet.getString("diagnosis"),
                resultSet.getString("advice")),
        patientId,
        limit);
  }

  private long countAppointmentsByStatus(LocalDate date, String status) {
    if (status == null) {
      return jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM appointments WHERE DATE(appointment_time) = ?", Long.class, date);
    }
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM appointments WHERE DATE(appointment_time) = ? AND status = ?",
        Long.class,
        date,
        status);
  }

  private long countNewPatients(LocalDate date) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM patients WHERE DATE(created_at) = ?", Long.class, date);
  }

  private static int normalizeTimelineLimit(int limit) {
    if (limit <= 0) {
      return 50;
    }
    return Math.min(limit, 200);
  }

  private List<PatientMedicalVisitMedicationView> findVisitMedications(String prescriptionId) {
    String sql =
        """
        SELECT pi.drug_id,
               d.drug_name,
               pi.quantity,
               pi.instructions
        FROM prescription_items pi
        JOIN drugs d ON d.drug_id = pi.drug_id
        WHERE pi.prescription_id = ?
        ORDER BY d.drug_name ASC
        """;

    return jdbcTemplate.query(
        sql,
        (resultSet, rowNum) ->
            new PatientMedicalVisitMedicationView(
                resultSet.getString("drug_id"),
                resultSet.getString("drug_name"),
                resultSet.getInt("quantity") + " unit(s)",
                resultSet.getInt("quantity"),
                resultSet.getString("instructions")),
        prescriptionId);
  }

  private record PatientSummaryRow(String patientId, String patientName) {}
}
